package com.example.workspace.search.application;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

import com.example.workspace.common.domain.SourceTables;
import com.example.workspace.common.domain.SourceType;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

@Component
public class SearchIndexer {
    private final DSLContext db;
    public SearchIndexer(DSLContext db) { this.db = db; }

    /** Called only in the worker's fenced transaction. Read current source under a row lock. */
    public void index(UUID workspace, SourceType type, UUID id, long generation) {
        var binding = SourceTables.of(type);
        var source = db.selectFrom(binding.table()).where(binding.identity(workspace, id)).forUpdate().fetchOne();
        var d = SEARCH_DOCUMENTS;
        if (source == null || source.get(binding.deletedAt()) != null
                || (type == SourceType.FILE && !"STORED".equals(source.get(FILES.STATE)))) {
            db.deleteFrom(d).where(d.INDEX_GENERATION.eq(generation).and(d.WORKSPACE_ID.eq(workspace))
                    .and(d.SOURCE_TYPE.eq(type.name())).and(d.SOURCE_ID.eq(id))).execute();
            return;
        }
        String title = source.get(binding.title());
        String body = type == SourceType.FILE ? "" : source.get(binding.body());
        long extraction = 0;
        if (type == SourceType.FILE) {
            extraction = source.get(FILES.EXTRACTION_GENERATION);
            body = db.select(FILE_EXTRACTIONS.TEXT_CONTENT).from(FILE_EXTRACTIONS)
                    .where(FILE_EXTRACTIONS.WORKSPACE_ID.eq(workspace).and(FILE_EXTRACTIONS.FILE_ID.eq(id))
                            .and(FILE_EXTRACTIONS.CONTENT_VERSION.eq(source.get(FILES.CONTENT_VERSION)))
                            .and(FILE_EXTRACTIONS.EXTRACTION_GENERATION.eq(extraction)).and(FILE_EXTRACTIONS.STATUS.eq("READY")))
                    .fetchOne(FILE_EXTRACTIONS.TEXT_CONTENT);
        }
        if (body == null) body = "";
        // Keep visible code and link labels, excluding markup and hidden destinations.
        if (type == SourceType.NOTE) body = body.replaceAll("(?s)<!--.*?-->", "").replaceAll("<[^>]+>", "")
                .replaceAll("!?\\[([^]]*)]\\([^)]*\\)", "$1").replaceAll("(?m)^#{1,6}\\s+", "").replace("```", "");
        body = SearchText.take(body, 200000);
        db.insertInto(d).set(d.INDEX_GENERATION, generation).set(d.WORKSPACE_ID, workspace)
                .set(d.SOURCE_TYPE, type.name()).set(d.SOURCE_ID, id).set(d.SOURCE_VERSION, source.get(binding.version()))
                .set(d.EXTRACTION_GENERATION, extraction).set(d.TITLE, title).set(d.BODY_TEXT, body)
                .set(d.NORMALIZED_TITLE, SearchText.normalize(title)).set(d.NORMALIZED_BODY, SearchText.normalize(body))
                .set(d.SEARCH_BIGRAMS, SearchText.bigrams(title, body)).set(d.SOURCE_UPDATED_AT, source.get(binding.updatedAt()))
                .onConflict(d.INDEX_GENERATION, d.WORKSPACE_ID, d.SOURCE_TYPE, d.SOURCE_ID).doUpdate()
                .set(d.SOURCE_VERSION, excluded(d.SOURCE_VERSION)).set(d.EXTRACTION_GENERATION, excluded(d.EXTRACTION_GENERATION))
                .set(d.TITLE, excluded(d.TITLE)).set(d.BODY_TEXT, excluded(d.BODY_TEXT))
                .set(d.NORMALIZED_TITLE, excluded(d.NORMALIZED_TITLE)).set(d.NORMALIZED_BODY, excluded(d.NORMALIZED_BODY))
                .set(d.SEARCH_BIGRAMS, excluded(d.SEARCH_BIGRAMS)).set(d.SOURCE_UPDATED_AT, excluded(d.SOURCE_UPDATED_AT))
                .set(d.INDEXED_AT, currentOffsetDateTime())
                .where(d.SOURCE_VERSION.le(excluded(d.SOURCE_VERSION)))
                .execute();
    }
}
