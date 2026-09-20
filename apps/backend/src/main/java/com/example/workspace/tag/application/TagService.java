package com.example.workspace.tag.application;

import static com.example.workspace.common.domain.SourceAccess.*;
import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.domain.SourceTables;
import com.example.workspace.common.domain.SourceType;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.Timestamps;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.tag.dto.TagDtos.*;
import com.example.workspace.tag.dto.TagDtos.Collection;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import java.text.Normalizer;
import java.util.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private final DSLContext db;
    private final CurrentWorkspaceResolver workspaces;
    private final SourceAccess access;
    public TagService(DSLContext db, CurrentWorkspaceResolver workspaces, SourceAccess access) {
        this.db = db; this.workspaces = workspaces; this.access = access;
    }
    public static String displayName(String input) {
        if (input == null) throw invalid("请输入标签名称");
        String name = input.strip();
        int length = name.codePointCount(0, name.length());
        if (length < 1 || length > 30 || name.codePoints().anyMatch(Character::isISOControl)) throw invalid("标签名称为 1–30 个字符且不能包含控制字符");
        if (normalizedName(name).isBlank() || normalizedName(name).length() > 100) throw invalid("标签名称无效");
        return name;
    }
    public static String normalizedName(String name) { return Normalizer.normalize(name, Normalizer.Form.NFKC).strip().toLowerCase(Locale.ROOT); }

    @Transactional(readOnly = true)
    public PageResponse<Tag> list(CurrentUser user, String q, PageQuery page) {
        UUID ws = workspaces.require(user).workspace().id();
        String term = q == null ? "" : normalizedName(q);
        if (term.codePointCount(0, term.length()) > 100) throw invalid("标签查询过长");
        var condition = TAGS.WORKSPACE_ID.eq(ws).and(position(TAGS.NORMALIZED_NAME, term).gt(0));
        var rows = db.selectFrom(TAGS).where(condition).orderBy(TAGS.NORMALIZED_NAME, TAGS.ID)
                .limit(page.sizeOrDefault()).offset(page.offset()).fetch();
        long total = db.selectCount().from(TAGS).where(condition).fetchSingle(0, long.class);
        return PageResponse.of(rows.map(r -> toTag(ws, r)), page, total);
    }
    @Transactional
    public Tag create(CurrentUser user, Create request) {
        UUID ws = workspaces.require(user).workspace().id();
        String name = displayName(request.name());
        unique(ws, name, null);
        if (db.fetchCount(TAGS, TAGS.WORKSPACE_ID.eq(ws)) >= 500) throw invalid("工作空间最多 500 个标签");
        return toTag(ws, db.insertInto(TAGS).set(TAGS.ID, UuidV7.next()).set(TAGS.WORKSPACE_ID, ws)
                .set(TAGS.NAME, name).set(TAGS.NORMALIZED_NAME, normalizedName(name))
                .set(TAGS.COLOR, request.color() == null ? "GRAY" : request.color().name()).returning().fetchSingle());
    }
    @Transactional
    public Tag update(CurrentUser user, UUID id, Update request) {
        UUID ws = workspaces.require(user).workspace().id();
        var old = requireTag(ws, id);
        if (!Objects.equals(old.get(TAGS.VERSION), request.version())) throw conflict(ErrorCode.VERSION_CONFLICT, "标签已被修改，请刷新后重试");
        String name = request.name() == null ? old.get(TAGS.NAME) : displayName(request.name());
        unique(ws, name, id);
        return toTag(ws, db.update(TAGS).set(TAGS.NAME, name).set(TAGS.NORMALIZED_NAME, normalizedName(name))
                .set(TAGS.COLOR, request.color() == null ? old.get(TAGS.COLOR) : request.color().name())
                .set(TAGS.VERSION, TAGS.VERSION.plus(1)).set(TAGS.UPDATED_AT, currentOffsetDateTime())
                .where(TAGS.WORKSPACE_ID.eq(ws).and(TAGS.ID.eq(id))).returning().fetchSingle());
    }
    @Transactional
    public void delete(CurrentUser user, UUID id, long version) {
        UUID ws = workspaces.require(user).workspace().id();
        var tag = requireTag(ws, id);
        if (tag.get(TAGS.VERSION) != version) throw conflict(ErrorCode.VERSION_CONFLICT, "标签已被修改，请刷新后重试");
        for (var type : SourceType.values()) {
            var source = SourceTables.of(type);
            var relation = source.tags();
            db.update(source.table()).set(source.tagVersion(), source.tagVersion().plus(1))
                    .where(source.workspaceId().eq(ws).and(source.id().in(select(relation.sourceId())
                            .from(relation.table()).where(relation.workspaceId().eq(ws).and(relation.tagId().eq(id)))))).execute();
        }
        db.deleteFrom(TAGS).where(TAGS.WORKSPACE_ID.eq(ws).and(TAGS.ID.eq(id))).execute();
    }
    @Transactional(readOnly = true)
    public Collection get(CurrentUser user, SourceType type, UUID id) {
        UUID ws = workspaces.require(user).workspace().id();
        var source = access.require(ws, type, id, false);
        return new Collection(tags(ws, type, id), source.get(SourceTables.of(type).tagVersion()));
    }
    @Transactional
    public Collection replace(CurrentUser user, SourceType type, UUID id, Replace request) {
        UUID ws = workspaces.require(user).workspace().id();
        var binding = SourceTables.of(type);
        var source = access.require(ws, type, id, true);
        long current = source.get(binding.tagVersion());
        if (current != request.tagVersion()) throw conflict(ErrorCode.TAG_VERSION_CONFLICT, "标签集合已改变，请刷新后核对");
        Set<UUID> ids = new LinkedHashSet<>(request.tagIds());
        if (ids.size() > 20) throw invalid("每个对象最多 20 个标签");
        for (var tag : ids) requireTag(ws, tag);
        var relation = binding.tags();
        db.deleteFrom(relation.table()).where(relation.workspaceId().eq(ws).and(relation.sourceId().eq(id))).execute();
        for (var tag : ids) db.insertInto(relation.table()).set(relation.workspaceId(), ws)
                .set(relation.sourceId(), id).set(relation.tagId(), tag).execute();
        db.update(binding.table()).set(binding.tagVersion(), binding.tagVersion().plus(1))
                .where(binding.identity(ws, id)).execute();
        return new Collection(tags(ws, type, id), current + 1);
    }
    public List<Tag> tags(UUID ws, SourceType type, UUID id) {
        var relation = SourceTables.of(type).tags();
        return db.select(TAGS.fields()).from(TAGS).join(relation.table())
                .on(relation.workspaceId().eq(TAGS.WORKSPACE_ID).and(relation.tagId().eq(TAGS.ID)))
                .where(relation.workspaceId().eq(ws).and(relation.sourceId().eq(id)))
                .orderBy(TAGS.NORMALIZED_NAME, TAGS.ID).fetch(r -> toTag(ws, r));
    }
    private Record requireTag(UUID ws, UUID id) {
        var row = db.selectFrom(TAGS).where(TAGS.WORKSPACE_ID.eq(ws).and(TAGS.ID.eq(id))).fetchOne();
        if (row == null) throw missing();
        return row;
    }
    private void unique(UUID ws, String name, UUID except) {
        var existing = db.select(TAGS.ID).from(TAGS).where(TAGS.WORKSPACE_ID.eq(ws)
                .and(TAGS.NORMALIZED_NAME.eq(normalizedName(name)))).fetchOne(TAGS.ID);
        if (existing != null && !existing.equals(except)) throw conflict(ErrorCode.TAG_NAME_CONFLICT, "同名标签已存在，请选择已有标签");
    }
    private Tag toTag(UUID ws, Record row) {
        long count = 0;
        for (var type : SourceType.values()) {
            var source = SourceTables.of(type);
            var relation = source.tags();
            count += db.selectCount().from(relation.table()).join(source.table())
                    .on(source.workspaceId().eq(relation.workspaceId()).and(source.id().eq(relation.sourceId())))
                    .where(relation.workspaceId().eq(ws).and(relation.tagId().eq(row.get(TAGS.ID)))
                            .and(source.readable(type))).fetchSingle(0, long.class);
        }
        return new Tag(row.get(TAGS.ID), row.get(TAGS.NAME), Color.valueOf(row.get(TAGS.COLOR)),
                row.get(TAGS.VERSION), count, Timestamps.toInstant(row.get(TAGS.CREATED_AT)), Timestamps.toInstant(row.get(TAGS.UPDATED_AT)));
    }
}
