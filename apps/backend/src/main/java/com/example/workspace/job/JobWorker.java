package com.example.workspace.job;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

import com.example.workspace.common.domain.SourceType;
import com.example.workspace.search.application.SearchIndexer;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.jooq.types.DayToSecond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class JobWorker {
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private final DSLContext db;
    private final TransactionTemplate tx;
    private final SearchIndexer indexer;
    public JobWorker(DSLContext db, PlatformTransactionManager transactions, SearchIndexer indexer) {
        this.db = db; this.tx = new TransactionTemplate(transactions); this.indexer = indexer;
    }
    // CURRENT_TIMESTAMP freezes at transaction start, before a workspace lock may have waited.
    private static Field<OffsetDateTime> now() {
        return function(name("clock_timestamp"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    }
    @Scheduled(fixedDelayString = "${app.jobs.poll-interval:1000}")
    public void tick() {
        var j = BACKGROUND_JOBS;
        var searchJob = j.JOB_TYPE.in("UPSERT_SEARCH", "REMOVE_SEARCH");
        // A worker can die on its last attempt. Such an expired lease must not stay RUNNING forever.
        db.update(j).set(j.STATUS, "DEAD").setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL)
                .set(j.ERROR_CODE, "INDEX_FAILURE").set(j.UPDATED_AT, now())
                .where(searchJob.and(j.ATTEMPTS.ge(5)).and(j.STATUS.eq("RUNNING")).and(j.LEASE_UNTIL.lt(now()))).execute();
        for (int i = 0; i < 20; i++) {
            UUID token = UUID.randomUUID();
            var job = tx.execute(s -> {
                var candidate = db.select(j.ID).from(j).where(searchJob.and(j.ATTEMPTS.lt(5)).and(
                        j.STATUS.eq("PENDING").and(j.NEXT_RUN_AT.le(now()))
                                .or(j.STATUS.eq("RUNNING").and(j.LEASE_UNTIL.lt(now())))))
                        .orderBy(j.NEXT_RUN_AT, j.ID).limit(1).forUpdate().skipLocked().fetchOne(j.ID);
                if (candidate == null) return null;
                return db.update(j).set(j.STATUS, "RUNNING").set(j.ATTEMPTS, j.ATTEMPTS.plus(1))
                        .set(j.LEASE_TOKEN, token).set(j.LEASE_UNTIL, now().add(new DayToSecond(0, 0, 1, 30)))
                        .set(j.UPDATED_AT, now()).where(j.ID.eq(candidate)).returning().fetchSingle();
            });
            if (job == null) return;
            try {
                tx.executeWithoutResult(s -> {
                    // Same lock order as source writes. No object storage or parser I/O in this transaction.
                    db.select(WORKSPACES.ID).from(WORKSPACES).where(WORKSPACES.ID.eq(job.getWorkspaceId())).forUpdate().fetch();
                    var owned = j.ID.eq(job.getId()).and(j.STATUS.eq("RUNNING")).and(j.LEASE_TOKEN.eq(token)).and(j.LEASE_UNTIL.gt(now()));
                    if (db.select(j.ID).from(j).where(owned).forUpdate().fetchOne() == null) return;
                    indexer.index(job.getWorkspaceId(), SourceType.valueOf(job.getSourceType()), job.getSourceId(), job.getGeneration());
                    int completed = db.update(j).set(j.STATUS, "SUCCEEDED").setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL)
                            .setNull(j.ERROR_CODE).set(j.UPDATED_AT, now()).where(owned).execute();
                    // Fence the projection and acknowledgement together if the lease elapsed during indexing.
                    if (completed != 1) s.setRollbackOnly();
                });
            } catch (Exception e) {
                log.warn("Search indexing failed jobId={} errorType={}", job.getId(), e.getClass().getSimpleName());
                db.update(j).set(j.STATUS, when(j.ATTEMPTS.ge(5), "DEAD").otherwise("PENDING"))
                        .setNull(j.LEASE_TOKEN).setNull(j.LEASE_UNTIL).set(j.ERROR_CODE, "INDEX_FAILURE")
                        .set(j.NEXT_RUN_AT, now().add(new DayToSecond(0, 0, 0, 10))).set(j.UPDATED_AT, now())
                        .where(j.ID.eq(job.getId()).and(j.STATUS.eq("RUNNING")).and(j.LEASE_TOKEN.eq(token))).execute();
            }
        }
    }
}
