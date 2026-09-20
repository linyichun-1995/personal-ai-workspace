package com.example.workspace.file.application;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@EnableScheduling
public class FileExtractionWorker {
    private final DSLContext db;
    private final TransactionTemplate tx;
    private final IsolatedFileParser parser;
    private final boolean enabled;
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("extraction-leases").factory());
    public FileExtractionWorker(DSLContext db, PlatformTransactionManager manager, IsolatedFileParser parser,
                                @Value("${app.file-extraction.enabled:false}") boolean enabled) {
        this.db = db; this.tx = new TransactionTemplate(manager); this.parser = parser; this.enabled = enabled;
    }
    private OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }
    @Scheduled(fixedDelayString = "${app.file-extraction.poll-interval:1000}")
    public void tick() { if (enabled) runOne(); }
    public boolean runOne() {
        var j = BACKGROUND_JOBS;
        UUID token = UUID.randomUUID();
        var job = tx.execute(s -> {
            var candidate = db.selectFrom(j).where(j.JOB_TYPE.eq("EXTRACT_FILE"),
                    j.STATUS.eq("PENDING").and(j.NEXT_RUN_AT.le(now())).or(j.STATUS.eq("RUNNING").and(j.LEASE_UNTIL.lt(now()))))
                    .orderBy(j.NEXT_RUN_AT, j.ID).limit(1).forUpdate().skipLocked().fetchOne();
            if (candidate == null) return null;
            return db.update(j).set(j.STATUS, "RUNNING").set(j.ATTEMPTS, j.ATTEMPTS.plus(1)).set(j.LEASE_TOKEN, token)
                    .set(j.LEASE_UNTIL, now().plusSeconds(90)).set(j.UPDATED_AT, now()).where(j.ID.eq(candidate.getId())).returning().fetchSingle();
        });
        if (job == null) return false;
        var renewal = heartbeat.scheduleAtFixedRate(() -> {
            try { db.update(j).set(j.LEASE_UNTIL, now().plusSeconds(90)).where(owned(job.getId(), token)).execute(); }
            catch (RuntimeException ignored) { /* Publishing rechecks the lease. */ }
        }, 20, 20, TimeUnit.SECONDS);
        try {
            var file = tx.execute(s -> {
                lockWorkspace(job.getWorkspaceId());
                var current = db.selectFrom(FILES).where(FILES.WORKSPACE_ID.eq(job.getWorkspaceId()), FILES.ID.eq(job.getSourceId()),
                        FILES.STATE.eq("STORED"), FILES.EXTRACTION_GENERATION.eq(job.getGeneration())).fetchOne();
                if (current != null && db.fetchExists(j, owned(job.getId(), token))) db.update(FILES).set(FILES.EXTRACTION_STATUS, "PROCESSING").where(FILES.ID.eq(current.getId())).execute();
                return current;
            });
            var result = file == null ? null : job.getAttempts() > 3
                    ? new IsolatedFileParser.Result("FAILED", "PARSER_UNAVAILABLE", false, new byte[0])
                    : parser.parse(FileCatalog.ref(file), file.getExtension(), file.getSizeBytes(), file.getSha256(), false);
            tx.executeWithoutResult(s -> {
                lockWorkspace(job.getWorkspaceId());
                if (db.select(j.ID).from(j).where(owned(job.getId(), token)).forUpdate().fetchOne() == null) return;
                boolean valid = file != null && db.fetchExists(FILES, FILES.ID.eq(file.getId()).and(FILES.STATE.eq("STORED"))
                        .and(FILES.CONTENT_VERSION.eq(file.getContentVersion())).and(FILES.EXTRACTION_GENERATION.eq(job.getGeneration())));
                if (valid) {
                    boolean retry = result.retryable() && job.getAttempts() < 3;
                    db.update(FILES).set(FILES.EXTRACTION_STATUS, retry ? "QUEUED" : result.status())
                            .set(FILES.EXTRACTION_ERROR_CODE, result.error().isEmpty() ? null : result.error())
                            .set(FILES.EXTRACTION_RETRYABLE, result.retryable()).where(FILES.ID.eq(file.getId())).execute();
                    if (retry) {
                        int changed = db.update(j).set(j.STATUS, "PENDING").setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL)
                                .set(j.ERROR_CODE, result.error()).set(j.NEXT_RUN_AT, now().plusSeconds(job.getAttempts() == 1 ? 10 : 60))
                                .where(owned(job.getId(), token)).execute();
                        if (changed != 1) s.setRollbackOnly();
                        return;
                    }
                    db.insertInto(FILE_EXTRACTIONS).set(FILE_EXTRACTIONS.WORKSPACE_ID, file.getWorkspaceId()).set(FILE_EXTRACTIONS.FILE_ID, file.getId())
                            .set(FILE_EXTRACTIONS.CONTENT_VERSION, file.getContentVersion()).set(FILE_EXTRACTIONS.EXTRACTION_GENERATION, job.getGeneration())
                            .set(FILE_EXTRACTIONS.PARSER_VERSION, "pdfbox-3.0.8/docx-stax-1/imageio-3.15.2")
                            .set(FILE_EXTRACTIONS.STATUS, result.status()).set(FILE_EXTRACTIONS.TEXT_CONTENT, result.status().equals("READY") ? result.text() : null)
                            .set(FILE_EXTRACTIONS.TEXT_LENGTH, result.text().codePointCount(0, result.text().length()))
                            .set(FILE_EXTRACTIONS.TRUNCATED, result.truncated()).set(FILE_EXTRACTIONS.ERROR_CODE, result.error())
                            .set(FILE_EXTRACTIONS.STARTED_AT, job.getUpdatedAt()).set(FILE_EXTRACTIONS.FINISHED_AT, now()).onConflictDoNothing().execute();
                }
                int count = db.update(j).set(j.STATUS, valid && result.status().equals("FAILED") ? "DEAD" : "SUCCEEDED")
                        .setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL).set(j.UPDATED_AT, now()).where(owned(job.getId(), token)).execute();
                if (count != 1) s.setRollbackOnly();
            });
        } finally { renewal.cancel(false); }
        return true;
    }
    private Condition owned(UUID id, UUID token) {
        return BACKGROUND_JOBS.ID.eq(id).and(BACKGROUND_JOBS.LEASE_TOKEN.eq(token))
                .and(BACKGROUND_JOBS.STATUS.eq("RUNNING")).and(BACKGROUND_JOBS.LEASE_UNTIL.gt(now()));
    }
    private void lockWorkspace(UUID ws) { db.select(WORKSPACES.ID).from(WORKSPACES).where(WORKSPACES.ID.eq(ws)).forUpdate().fetch(); }
    @PreDestroy public void close() { heartbeat.shutdownNow(); }
}
