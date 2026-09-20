package com.example.workspace.file.application;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import java.time.*;
import java.util.*;
import com.example.workspace.infrastructure.storage.*;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Reconciliation uses persisted exact references, never a filesystem or whole-bucket delete. */
@Component
public class FileMaintenance {
    private final DSLContext db;
    private final TransactionTemplate tx;
    private final ObjectStorage storage;
    private final boolean purgeEnabled;
    public FileMaintenance(DSLContext db, PlatformTransactionManager manager, ObjectStorage storage,
                            @Value("${app.file-purge.enabled:false}") boolean purgeEnabled) {
        this.db=db;this.tx=new TransactionTemplate(manager);this.storage=storage;this.purgeEnabled=purgeEnabled;
    }
    private OffsetDateTime now() {return OffsetDateTime.now(ZoneOffset.UTC);}
    @Scheduled(fixedDelayString="${app.files.maintenance-interval:60000}")
    public void tick() {
        expireUploads();
        if (purgeEnabled) { cleanupCandidates(); purgeFiles(); }
    }
    public void expireUploads() {
        var u=FILE_UPLOADS;
        var expired=u.STATE.in("CREATED","RECEIVING","VERIFYING").and(u.EXPIRES_AT.lt(now()))
                .and(u.LEASE_UNTIL.isNull().or(u.LEASE_UNTIL.lt(now())));
        for(var upload:db.selectFrom(u).where(expired).limit(100).fetch()) tx.executeWithoutResult(s->{
            lock(upload.getWorkspaceId());
            var current=db.selectFrom(u).where(u.ID.eq(upload.getId()),expired).forUpdate().fetchOne();
            if(current==null)return;
            db.update(WORKSPACE_STORAGE_USAGE).set(WORKSPACE_STORAGE_USAGE.RESERVED_BYTES,WORKSPACE_STORAGE_USAGE.RESERVED_BYTES.minus(current.getReservedBytes()))
                    .set(WORKSPACE_STORAGE_USAGE.VERSION,WORKSPACE_STORAGE_USAGE.VERSION.plus(1)).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(current.getWorkspaceId())).execute();
            db.update(u).set(u.STATE,"EXPIRED").set(u.RESERVED_BYTES,0L).setNull(u.LEASE_TOKEN).setNull(u.LEASE_UNTIL).where(u.ID.eq(current.getId())).execute();
            db.update(FILES).set(FILES.STATE,"FAILED").where(FILES.ID.eq(current.getFileId()),FILES.STATE.eq("UPLOADING")).execute();
            db.update(FILE_UPLOAD_ATTEMPTS).set(FILE_UPLOAD_ATTEMPTS.STATE,"ORPHANED")
                    .where(FILE_UPLOAD_ATTEMPTS.UPLOAD_ID.eq(current.getId()),FILE_UPLOAD_ATTEMPTS.STATE.notIn("COMMITTED","CLEANED")).execute();
        });
    }
    public void cleanupCandidates() {
        var a=FILE_UPLOAD_ATTEMPTS;
        for(var attempt:db.selectFrom(a).where(a.STATE.notIn("COMMITTED","CLEANED"),a.CLEANUP_AFTER.lt(now())).limit(100).fetch()) {
            boolean safe=tx.execute(s->{
                lock(attempt.getWorkspaceId());
                var u=db.selectFrom(FILE_UPLOADS).where(FILE_UPLOADS.ID.eq(attempt.getUploadId())).fetchSingle();
                return (u.getLeaseUntil()==null || u.getLeaseUntil().isBefore(now())) && u.getExpiresAt().isBefore(now())
                        && !db.fetchExists(FILES,FILES.STORAGE_PROFILE_ID.eq(attempt.getStorageProfileId()).and(FILES.STORAGE_BUCKET.eq(attempt.getStorageBucket())).and(FILES.OBJECT_KEY.eq(attempt.getObjectKey())));
            });
            if(!safe)continue;
            try {
                storage.delete(FileCatalog.ref(attempt));
                db.update(a).set(a.STATE,"CLEANED").setNull(a.LAST_ERROR_CODE).where(a.ID.eq(attempt.getId()),a.STATE.ne("COMMITTED")).execute();
            }catch(StorageException e){db.update(a).set(a.LAST_ERROR_CODE,e.code().name()).where(a.ID.eq(attempt.getId())).execute();}
        }
    }
    public void purgeFiles() {
        var j = BACKGROUND_JOBS;
        for (var file : db.selectFrom(FILES).where(FILES.STATE.eq("DELETED"), FILES.PURGE_AFTER.lt(now()),
                org.jooq.impl.DSL.notExists(db.selectOne().from(j).where(j.JOB_TYPE.eq("PURGE_FILE"), j.SOURCE_ID.eq(FILES.ID))))
                .limit(100).fetch()) {
            db.insertInto(j).set(j.ID, UUID.randomUUID()).set(j.WORKSPACE_ID, file.getWorkspaceId())
                    .set(j.JOB_TYPE, "PURGE_FILE").set(j.SOURCE_TYPE, "FILE").set(j.SOURCE_ID, file.getId())
                    .set(j.SOURCE_VERSION, file.getVersion()).set(j.DEDUPE_KEY, "purge:" + file.getId())
                    .onConflictDoNothing().execute();
        }
        db.update(j).set(j.STATUS, "DEAD").setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL).set(j.ERROR_CODE, "PURGE_FAILURE")
                .where(j.JOB_TYPE.eq("PURGE_FILE"), j.STATUS.eq("RUNNING"), j.ATTEMPTS.ge(5), j.LEASE_UNTIL.lt(now())).execute();
        for (int i = 0; i < 20; i++) {
            UUID token = UUID.randomUUID();
            var job = tx.execute(s -> {
                var candidate = db.selectFrom(j).where(j.JOB_TYPE.eq("PURGE_FILE"), j.ATTEMPTS.lt(5),
                        j.STATUS.eq("PENDING").and(j.NEXT_RUN_AT.le(now())).or(j.STATUS.eq("RUNNING").and(j.LEASE_UNTIL.lt(now()))))
                        .orderBy(j.NEXT_RUN_AT, j.ID).limit(1).forUpdate().skipLocked().fetchOne();
                if (candidate == null) return null;
                return db.update(j).set(j.STATUS, "RUNNING").set(j.ATTEMPTS, j.ATTEMPTS.plus(1)).set(j.LEASE_TOKEN, token)
                        .set(j.LEASE_UNTIL, now().plusSeconds(90)).where(j.ID.eq(candidate.getId())).returning().fetchSingle();
            });
            if (job == null) return;
            try {
                // Restore is refused after purge_after. Object I/O stays outside database transactions.
                var file = db.selectFrom(FILES).where(FILES.ID.eq(job.getSourceId()), FILES.WORKSPACE_ID.eq(job.getWorkspaceId()),
                        FILES.STATE.eq("DELETED"), FILES.PURGE_AFTER.lt(now())).fetchOne();
                if (file != null) storage.delete(FileCatalog.ref(file));
                tx.executeWithoutResult(s -> {
                    lock(job.getWorkspaceId());
                    if (db.select(j.ID).from(j).where(purgeLease(job.getId(), token)).forUpdate().fetchOne() == null) return;
                    if (file != null) {
                        int changed = db.update(FILES).set(FILES.STATE, "PURGED").set(FILES.PURGED_AT, now())
                                .where(FILES.ID.eq(file.getId()), FILES.STATE.eq("DELETED"), FILES.PURGE_AFTER.lt(now())).execute();
                        if (changed == 1) {
                            db.deleteFrom(TASK_FILES).where(TASK_FILES.FILE_ID.eq(file.getId())).execute();
                            db.deleteFrom(NOTE_FILES).where(NOTE_FILES.FILE_ID.eq(file.getId())).execute();
                            db.deleteFrom(FILE_EXTRACTIONS).where(FILE_EXTRACTIONS.FILE_ID.eq(file.getId())).execute();
                            db.deleteFrom(FILE_TAGS).where(FILE_TAGS.SOURCE_ID.eq(file.getId())).execute();
                            db.update(WORKSPACE_STORAGE_USAGE).set(WORKSPACE_STORAGE_USAGE.USED_BYTES, WORKSPACE_STORAGE_USAGE.USED_BYTES.minus(file.getSizeBytes()))
                                    .set(WORKSPACE_STORAGE_USAGE.VERSION, WORKSPACE_STORAGE_USAGE.VERSION.plus(1))
                                    .where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(file.getWorkspaceId())).execute();
                        }
                    }
                    int completed = db.update(j).set(j.STATUS, "SUCCEEDED").setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL)
                            .setNull(j.ERROR_CODE).set(j.UPDATED_AT, now()).where(purgeLease(job.getId(), token)).execute();
                    if (completed != 1) s.setRollbackOnly();
                });
            } catch (RuntimeException error) {
                String code = error instanceof StorageException storageError ? storageError.code().name() : "PURGE_FAILURE";
                db.update(j).set(j.STATUS, job.getAttempts() >= 5 ? "DEAD" : "PENDING").setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL)
                        .set(j.ERROR_CODE, code).set(j.NEXT_RUN_AT, now().plusSeconds(10L * (1L << job.getAttempts())))
                        .set(j.UPDATED_AT, now()).where(purgeLease(job.getId(), token)).execute();
            }
        }
    }
    private org.jooq.Condition purgeLease(UUID id, UUID token) {
        return BACKGROUND_JOBS.ID.eq(id).and(BACKGROUND_JOBS.STATUS.eq("RUNNING"))
                .and(BACKGROUND_JOBS.LEASE_TOKEN.eq(token)).and(BACKGROUND_JOBS.LEASE_UNTIL.gt(now()));
    }
    private void lock(UUID ws){db.select(WORKSPACES.ID).from(WORKSPACES).where(WORKSPACES.ID.eq(ws)).forUpdate().fetch();}
}
