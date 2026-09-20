package com.example.workspace.search.application;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.domain.SourceType;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.domain.SourceTables;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.Timestamps;
import com.example.workspace.search.dto.SearchQuery;
import com.example.workspace.search.dto.SearchResponse;
import com.example.workspace.search.dto.SearchResponse.*;
import com.example.workspace.tag.application.TagService;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record5;
import org.jooq.Select;
import org.jooq.SortField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    private final DSLContext db;
    private final CurrentWorkspaceResolver workspaces;
    private final TagService tags;
    private final MeterRegistry metrics;
    private final long generation;
    private final boolean enabled;
    public SearchService(DSLContext db, CurrentWorkspaceResolver workspaces, TagService tags, MeterRegistry metrics,
            @Value("${app.search.index-generation:1}") long generation, @Value("${app.search.enabled:true}") boolean enabled) {
        this.db = db; this.workspaces = workspaces; this.tags = tags; this.metrics = metrics;
        this.generation = generation; this.enabled = enabled;
    }

    /** The projection is only a candidate index: lifecycle, project scope and labels stay live. */
    private Select<Record5<String, UUID, UUID, UUID, Boolean>> visibleSources(UUID workspace) {
        Select<Record5<String, UUID, UUID, UUID, Boolean>> sources = null;
        for (var type : SourceType.values()) {
            var s = SourceTables.of(type);
            var p = PROJECTS.as("parent");
            var branch = select(inline(type.name()).as("type"), s.workspaceId().as("workspace_id"),
                    s.id().as("id"), s.projectId().as("project_id"),
                    field(s.archived().or(p.ARCHIVED_AT.isNotNull())).as("archived"))
                    .from(s.table()).leftJoin(p).on(p.WORKSPACE_ID.eq(s.workspaceId()).and(p.ID.eq(s.projectId())))
                    .where(s.workspaceId().eq(workspace).and(s.readable(type)));
            sources = sources == null ? branch : sources.unionAll(branch);
        }
        return sources;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SearchResponse search(CurrentUser user, SearchQuery query) {
        long started = System.nanoTime();
        if (!enabled) throw unavailable();
        UUID ws = workspaces.require(user).workspace().id();
        var terms = SearchText.terms(query.q());
        Set<UUID> tagIds = query.tagIds() == null ? Set.of() : new LinkedHashSet<>(query.tagIds());
        if (tagIds.size() > 20) throw SourceAccess.invalid("最多筛选 20 个标签");
        if (query.updatedFrom() != null && query.updatedTo() != null && !query.updatedTo().isAfter(query.updatedFrom())) throw SourceAccess.invalid("结束时间必须晚于开始时间");
        if (query.sort() != null && !Set.of("relevance", "updatedAt,desc").contains(query.sort())) throw SourceAccess.invalid("搜索排序无效");
        boolean empty = terms.isEmpty() && (query.types() == null || query.types().isEmpty()) && query.projectId() == null
                && tagIds.isEmpty() && query.updatedFrom() == null && query.updatedTo() == null;
        if (empty) return new SearchResponse(List.of(), query.pageNumber(), query.pageSize(), 0, 0, new Meta(true, false, 0));
        var d = SEARCH_DOCUMENTS;
        var s = visibleSources(ws).asTable("sources");
        var sourceType = s.field(name("type"), String.class);
        var sourceWorkspace = s.field(name("workspace_id"), UUID.class);
        var sourceId = s.field(name("id"), UUID.class);
        var projectId = s.field(name("project_id"), UUID.class);
        var archived = s.field(name("archived"), Boolean.class);
        var p = PROJECTS.as("result_project");
        var from = d.join(s).on(sourceWorkspace.eq(d.WORKSPACE_ID).and(sourceType.eq(d.SOURCE_TYPE)).and(sourceId.eq(d.SOURCE_ID)))
                .leftJoin(p).on(p.WORKSPACE_ID.eq(sourceWorkspace).and(p.ID.eq(projectId)));
        Condition where = d.INDEX_GENERATION.eq(generation).and(d.WORKSPACE_ID.eq(ws));
        if (!Boolean.TRUE.equals(query.includeArchived())) where = where.and(archived.isFalse());
        if (query.types() != null && !query.types().isEmpty()) where = where.and(d.SOURCE_TYPE.in(query.types().stream().map(Enum::name).toList()));
        if (query.projectId() != null) where = where.and(projectId.eq(query.projectId()));
        if (query.updatedFrom() != null) where = where.and(d.SOURCE_UPDATED_AT.ge(Timestamps.toOffset(query.updatedFrom())));
        if (query.updatedTo() != null) where = where.and(d.SOURCE_UPDATED_AT.lt(Timestamps.toOffset(query.updatedTo())));
        if (!tagIds.isEmpty()) {
            Condition labels = falseCondition();
            for (var type : SourceType.values()) {
                var r = SourceTables.of(type).tags();
                var count = selectCount().from(r.table()).where(r.workspaceId().eq(d.WORKSPACE_ID)
                        .and(r.sourceId().eq(d.SOURCE_ID)).and(r.tagId().in(tagIds))).asField();
                labels = labels.or(d.SOURCE_TYPE.eq(type.name()).and(count.ge(query.tagMode() == SearchQuery.TagMode.ANY ? 1 : tagIds.size())));
            }
            where = where.and(labels);
        }
        Field<Integer> score = inline(0);
        if (!terms.isEmpty()) score = score.plus(when(d.NORMALIZED_TITLE.eq(String.join(" ", terms)), 100).otherwise(0));
        for (String term : terms) {
            // Array containment uses the GIN candidates; position verifies a literal contiguous hit.
            where = where.and(d.SEARCH_BIGRAMS.contains(SearchText.bigrams(term)))
                    .and(position(d.NORMALIZED_TITLE, term).gt(0).or(position(d.NORMALIZED_BODY, term).gt(0)));
            score = score.plus(when(position(d.NORMALIZED_TITLE, term).eq(1), 80)
                    .when(position(d.NORMALIZED_TITLE, term).gt(0), 60).otherwise(20));
        }
        try {
            long total = db.selectCount().from(from).where(where).queryTimeout(2).fetchSingle(0, long.class);
            List<SortField<?>> order = new ArrayList<>();
            if (!terms.isEmpty() && !"updatedAt,desc".equals(query.sort())) order.add(score.desc());
            order.add(d.SOURCE_UPDATED_AT.desc()); order.add(d.SOURCE_TYPE.asc()); order.add(d.SOURCE_ID.asc());
            var rows = db.select(d.fields()).select(archived, projectId, p.NAME.as("project_name"))
                    .from(from).where(where).orderBy(order).limit(query.pageSize()).offset((query.pageNumber() - 1) * query.pageSize())
                    .queryTimeout(2).fetch();
            var j = BACKGROUND_JOBS;
            boolean pending = db.fetchExists(selectOne().from(j).where(j.WORKSPACE_ID.eq(ws)
                    .and(j.JOB_TYPE.in("UPSERT_SEARCH", "REMOVE_SEARCH", "EXTRACT_FILE")).and(j.STATUS.in("PENDING", "RUNNING", "DEAD"))));
            return new SearchResponse(rows.map(r -> item(ws, r, terms, projectId, archived)), query.pageNumber(), query.pageSize(),
                    total, (int) ((total + query.pageSize() - 1) / query.pageSize()),
                    new Meta(false, pending, (System.nanoTime() - started) / 1_000_000));
        } catch (org.jooq.exception.DataAccessException | DataAccessException e) { throw unavailable(); }
        finally { metrics.timer("workspace.search.duration").record(System.nanoTime() - started, TimeUnit.NANOSECONDS); }
    }
    private Item item(UUID ws, Record row, List<String> terms, Field<UUID> projectId, Field<Boolean> archived) {
        var d = SEARCH_DOCUMENTS;
        var type = SourceType.valueOf(row.get(d.SOURCE_TYPE));
        UUID id = row.get(d.SOURCE_ID);
        List<String> fields = new ArrayList<>();
        if (terms.stream().anyMatch(row.get(d.NORMALIZED_TITLE)::contains)) fields.add("TITLE");
        if (terms.stream().anyMatch(row.get(d.NORMALIZED_BODY)::contains)) fields.add("BODY");
        String body = row.get(d.BODY_TEXT);
        return new Item(type, id, row.get(d.TITLE), row.get(projectId) == null ? null : new Project(row.get(projectId), row.get("project_name", String.class)),
                tags.tags(ws, type, id), SearchText.snippet(body.isEmpty() ? row.get(d.TITLE) : body, terms), fields,
                row.get(archived), Timestamps.toInstant(row.get(d.SOURCE_UPDATED_AT)));
    }
    private AppException unavailable() { return new AppException(ErrorCode.SEARCH_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "搜索暂时不可用，请稍后重试或缩小范围"); }
}
