package com.example.workspace.file.application;

import static com.example.workspace.common.domain.SourceAccess.*;
import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

import com.example.workspace.common.api.*;
import com.example.workspace.common.domain.*;
import com.example.workspace.common.exception.AppException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.Timestamps;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.file.dto.FileDtos.*;
import com.example.workspace.file.dto.FileDtos.File;
import com.example.workspace.infrastructure.database.jooq.tables.records.FilesRecord;
import com.example.workspace.infrastructure.storage.*;
import com.example.workspace.tag.application.TagService;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileCatalog {
    private final DSLContext db;
    private final CurrentWorkspaceResolver workspaces;
    private final SourceAccess access;
    private final TagService tags;
    private final FilePolicy policy;
    private final StorageProperties storageProperties;

    public FileCatalog(DSLContext db, CurrentWorkspaceResolver workspaces, SourceAccess access,
                       TagService tags, FilePolicy policy, StorageProperties properties) {
        this.db = db;
        this.workspaces = workspaces;
        this.access = access;
        this.tags = tags;
        this.policy = policy;
        this.storageProperties = properties;
    }

    @Transactional(readOnly = true)
    public Limits capabilities(CurrentUser user) {
        UUID ws = workspaces.require(user).workspace().id();
        var usage = db.selectFrom(WORKSPACE_STORAGE_USAGE).where(WORKSPACE_STORAGE_USAGE.WORKSPACE_ID.eq(ws)).fetchOne();
        return new Limits(FilePolicy.FORMATS, policy.maxBytes, 10, 2,
                usage == null ? policy.quotaBytes : usage.getQuotaBytes(),
                usage == null ? 0 : usage.getUsedBytes(), usage == null ? 0 : usage.getReservedBytes(), storageProperties.enabled());
    }

    @Transactional(readOnly = true)
    public File get(CurrentUser user, UUID id) {
        return describe(workspaces.require(user).workspace().id(), id);
    }

    public File describe(UUID ws, UUID id) {
        var file = require(ws, id);
        var parent = file.getProjectId() == null ? null : db.selectFrom(PROJECTS)
                .where(PROJECTS.WORKSPACE_ID.eq(ws), PROJECTS.ID.eq(file.getProjectId())).fetchOne();
        boolean valid = file.getProjectId() == null || (parent != null && parent.getDeletedAt() == null);
        boolean writable = valid && (parent == null || parent.getArchivedAt() == null);
        if (!file.getState().equals("DELETED") && !valid) throw missing();
        var extraction = db.select(FILE_EXTRACTIONS.TRUNCATED).from(FILE_EXTRACTIONS)
                .where(extractionScope(file)).fetchOne();
        var references = new ArrayList<Reference>();
        references.addAll(db.select(TASKS.ID, TASKS.TITLE).from(TASK_FILES).join(TASKS)
                .on(TASKS.WORKSPACE_ID.eq(TASK_FILES.WORKSPACE_ID), TASKS.ID.eq(TASK_FILES.TASK_ID))
                .where(TASK_FILES.WORKSPACE_ID.eq(ws), TASK_FILES.FILE_ID.eq(id), TASKS.DELETED_AT.isNull())
                .fetch(r -> new Reference("TASK", r.get(TASKS.ID), r.get(TASKS.TITLE))));
        references.addAll(db.select(NOTES.ID, NOTES.TITLE).from(NOTE_FILES).join(NOTES)
                .on(NOTES.WORKSPACE_ID.eq(NOTE_FILES.WORKSPACE_ID), NOTES.ID.eq(NOTE_FILES.NOTE_ID))
                .where(NOTE_FILES.WORKSPACE_ID.eq(ws), NOTE_FILES.FILE_ID.eq(id), NOTES.DELETED_AT.isNull())
                .fetch(r -> new Reference("NOTE", r.get(NOTES.ID), r.get(NOTES.TITLE))));
        Instant purge = Timestamps.toInstant(file.getPurgeAfter());
        return new File(id, file.getDisplayName(), file.getOriginalName(), file.getDetectedMediaType(),
                file.getSizeBytes(), file.getProjectId(), file.getState(),
                new Extraction(file.getExtractionStatus(), extraction != null && extraction.value1(),
                        file.getExtractionRetryable(), file.getExtractionErrorCode()),
                file.getVersion(), file.getTagVersion(), tags.tags(ws, SourceType.FILE, id), references,
                new Capabilities(writable && file.getState().equals("STORED"), valid && file.getState().equals("STORED"),
                        writable && file.getState().equals("DELETED") && purge != null && purge.isAfter(Instant.now())),
                Timestamps.toInstant(file.getCreatedAt()), Timestamps.toInstant(file.getUpdatedAt()),
                Timestamps.toInstant(file.getDeletedAt()), purge, file.getDeletionReason());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<File> list(CurrentUser user, String q, UUID projectId, List<UUID> tagIds, String tagMode,
            List<String> mediaTypes, String extractionStatus, Instant updatedFrom, Instant updatedTo,
            boolean uncategorized, boolean unassigned, boolean archived, String view, PageQuery page) {
        UUID ws = workspaces.require(user).workspace().id();
        if (q != null && q.codePointCount(0, q.length()) > 100) throw invalid("文件名查询最长 100 个字符");
        if (!Set.of("active", "trash").contains(view)) throw invalid("文件视图无效");
        if (!Set.of("ALL", "ANY").contains(tagMode)) throw invalid("标签匹配模式无效");
        if (updatedFrom != null && updatedTo != null && !updatedTo.isAfter(updatedFrom)) throw invalid("结束时间必须晚于开始时间");
        Condition condition = FILES.WORKSPACE_ID.eq(ws).and(FILES.STATE.eq(view.equals("trash") ? "DELETED" : "STORED"));
        if (view.equals("active")) {
            condition = condition.and(FILES.PROJECT_ID.isNull().or(PROJECTS.ID.isNotNull().and(PROJECTS.DELETED_AT.isNull())));
            if (!archived) condition = condition.and(PROJECTS.ARCHIVED_AT.isNull());
        }
        if (q != null && !q.isBlank()) condition = condition.and(position(lower(FILES.DISPLAY_NAME), lower(val(q.strip()))).gt(0));
        if (projectId != null) condition = condition.and(FILES.PROJECT_ID.eq(projectId));
        if (unassigned) condition = condition.and(FILES.PROJECT_ID.isNull());
        if (uncategorized) condition = condition.and(FILES.PROJECT_ID.isNull()).andNotExists(selectOne().from(FILE_TAGS)
                .where(FILE_TAGS.WORKSPACE_ID.eq(FILES.WORKSPACE_ID), FILE_TAGS.SOURCE_ID.eq(FILES.ID)));
        if (tagIds != null && !tagIds.isEmpty()) {
            var unique = new HashSet<>(tagIds);
            if (unique.size() > 20) throw invalid("最多筛选 20 个标签");
            condition = condition.and(selectCount().from(FILE_TAGS)
                    .where(FILE_TAGS.WORKSPACE_ID.eq(FILES.WORKSPACE_ID), FILE_TAGS.SOURCE_ID.eq(FILES.ID), FILE_TAGS.TAG_ID.in(unique))
                    .asField().ge(tagMode.equals("ANY") ? 1 : unique.size()));
        }
        if (mediaTypes != null && !mediaTypes.isEmpty()) condition = condition.and(FILES.DETECTED_MEDIA_TYPE.in(mediaTypes));
        if (extractionStatus != null) {
            if (!Set.of("NOT_STARTED", "QUEUED", "PROCESSING", "READY", "EMPTY", "SKIPPED", "FAILED").contains(extractionStatus)) throw invalid("处理状态无效");
            condition = condition.and(FILES.EXTRACTION_STATUS.eq(extractionStatus));
        }
        if (updatedFrom != null) condition = condition.and(FILES.UPDATED_AT.ge(Timestamps.toOffset(updatedFrom)));
        if (updatedTo != null) condition = condition.and(FILES.UPDATED_AT.lt(Timestamps.toOffset(updatedTo)));
        SortField<?> order = switch (page.sort() == null ? "updatedAt,desc" : page.sort()) {
            case "updatedAt,desc" -> FILES.UPDATED_AT.desc();
            case "name,asc" -> FILES.DISPLAY_NAME.asc();
            case "sizeBytes,desc" -> FILES.SIZE_BYTES.desc();
            default -> throw invalid("文件排序无效");
        };
        var from = FILES.leftJoin(PROJECTS).on(PROJECTS.WORKSPACE_ID.eq(FILES.WORKSPACE_ID), PROJECTS.ID.eq(FILES.PROJECT_ID));
        long total = db.selectCount().from(from).where(condition).fetchSingle().value1();
        var ids = db.select(FILES.ID).from(from).where(condition).orderBy(order, FILES.ID)
                .limit(page.sizeOrDefault()).offset(page.offset()).fetch(FILES.ID);
        return PageResponse.of(ids.stream().map(id -> describe(ws, id)).toList(), page, total);
    }

    @Transactional
    public File update(CurrentUser user, UUID id, long version, String name, boolean changeProject, UUID project) {
        UUID ws = workspaces.require(user).workspace().id();
        var file = writable(ws, id);
        checkVersion(file, version);
        if (changeProject && !Objects.equals(project, file.getProjectId())) {
            access.requireProject(ws, project, true);
            if (hasReferences(ws, id)) throw conflict(ErrorCode.FILE_HAS_REFERENCES, "请先解除全部附件引用，再修改项目归属");
        }
        db.update(FILES).set(FILES.DISPLAY_NAME, name == null ? file.getDisplayName() : FilePolicy.name(name))
                .set(FILES.PROJECT_ID, changeProject ? project : file.getProjectId()).set(FILES.VERSION, FILES.VERSION.plus(1))
                .set(FILES.UPDATED_AT, now()).where(scope(ws, id)).execute();
        return describe(ws, id);
    }

    @Transactional
    public void delete(CurrentUser user, UUID id, long version) {
        UUID ws = workspaces.require(user).workspace().id();
        var file = require(ws, id);
        access.requireProject(ws, file.getProjectId(), true);
        if (file.getState().equals("DELETED")) return;
        if (!file.getState().equals("STORED")) throw missing();
        checkVersion(file, version);
        var time = now();
        db.update(FILES).set(FILES.STATE, "DELETED").set(FILES.DELETED_AT, time).set(FILES.PURGE_AFTER, time.plusDays(30))
                .set(FILES.DELETION_REASON, "USER").set(FILES.VERSION, FILES.VERSION.plus(1)).set(FILES.UPDATED_AT, time)
                .where(scope(ws, id)).execute();
    }

    @Transactional
    public File restore(CurrentUser user, UUID id, long version) {
        UUID ws = workspaces.require(user).workspace().id();
        var file = require(ws, id);
        checkVersion(file, version);
        access.requireProject(ws, file.getProjectId(), true);
        if (!file.getState().equals("DELETED") || file.getPurgeAfter() == null || !file.getPurgeAfter().isAfter(now()))
            throw conflict(ErrorCode.RESOURCE_READ_ONLY, "文件已超过可恢复期限");
        db.update(FILES).set(FILES.STATE, "STORED").setNull(FILES.DELETED_AT).setNull(FILES.PURGE_AFTER).setNull(FILES.DELETION_REASON)
                .set(FILES.VERSION, FILES.VERSION.plus(1)).set(FILES.UPDATED_AT, now()).where(scope(ws, id)).execute();
        if (Set.of("QUEUED", "PROCESSING").contains(file.getExtractionStatus())) enqueueExtraction(ws, id, file.getExtractionGeneration() + 1);
        return describe(ws, id);
    }

    @Transactional
    public File retryExtraction(CurrentUser user, UUID id) {
        UUID ws = workspaces.require(user).workspace().id();
        var file = writable(ws, id);
        if (Set.of("QUEUED", "PROCESSING").contains(file.getExtractionStatus()) || !file.getExtractionRetryable())
            throw conflict(ErrorCode.UPLOAD_STATE_CONFLICT, "当前文件不需要重新处理");
        if (file.getExtractionRequestedAt() != null && file.getExtractionRequestedAt().plusSeconds(60).isAfter(now()))
            throw new AppException(ErrorCode.RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS, "请在上次处理请求 60 秒后重试");
        enqueueExtraction(ws, id, file.getExtractionGeneration() + 1);
        return describe(ws, id);
    }

    private void enqueueExtraction(UUID ws, UUID id, long generation) {
        db.update(FILES).set(FILES.EXTRACTION_GENERATION, generation).set(FILES.EXTRACTION_STATUS, "QUEUED")
                .set(FILES.EXTRACTION_REQUESTED_AT, now()).setNull(FILES.EXTRACTION_ERROR_CODE).set(FILES.EXTRACTION_RETRYABLE, false)
                .where(scope(ws, id)).execute();
        db.insertInto(BACKGROUND_JOBS).set(BACKGROUND_JOBS.ID, UuidV7.next()).set(BACKGROUND_JOBS.WORKSPACE_ID, ws)
                .set(BACKGROUND_JOBS.JOB_TYPE, "EXTRACT_FILE").set(BACKGROUND_JOBS.SOURCE_TYPE, "FILE").set(BACKGROUND_JOBS.SOURCE_ID, id)
                .set(BACKGROUND_JOBS.SOURCE_VERSION, 1L).set(BACKGROUND_JOBS.GENERATION, generation)
                .set(BACKGROUND_JOBS.DEDUPE_KEY, "extract:" + id + ":" + generation).onConflictDoNothing().execute();
    }

    @Transactional(readOnly = true)
    public ObjectStorage.Ref contentRef(CurrentUser user, UUID id) {
        UUID ws = workspaces.require(user).workspace().id();
        access.require(ws, SourceType.FILE, id, false);
        return ref(require(ws, id));
    }

    public record ParserInput(ObjectStorage.Ref ref, String extension, long size, String sha256) {}
    @Transactional(readOnly = true)
    public ParserInput parserInput(CurrentUser user, UUID id) {
        UUID ws = workspaces.require(user).workspace().id();
        access.require(ws, SourceType.FILE, id, false);
        var file = require(ws, id);
        return new ParserInput(ref(file), file.getExtension(), file.getSizeBytes(), file.getSha256());
    }

    @Transactional(readOnly = true)
    public Text text(CurrentUser user, UUID id) {
        UUID ws = workspaces.require(user).workspace().id();
        access.require(ws, SourceType.FILE, id, false);
        var file = require(ws, id);
        var text = db.select(FILE_EXTRACTIONS.TEXT_CONTENT, FILE_EXTRACTIONS.TRUNCATED).from(FILE_EXTRACTIONS)
                .where(extractionScope(file), FILE_EXTRACTIONS.STATUS.eq("READY")).fetchOne();
        return new Text(file.getExtractionStatus(), text == null ? null : text.value1(), text != null && text.value2());
    }

    @Transactional(readOnly = true)
    public PageResponse<File> attachments(CurrentUser user, SourceType type, UUID id, PageQuery page) {
        UUID ws = workspaces.require(user).workspace().id();
        access.require(ws, type, id, false);
        var link = attachment(type);
        var from = link.table().join(FILES).on(FILES.WORKSPACE_ID.eq(link.workspace()), FILES.ID.eq(link.file()));
        var filter = link.workspace().eq(ws).and(link.source().eq(id)).and(FILES.STATE.in("STORED", "DELETED"));
        long total = db.selectCount().from(from).where(filter).fetchSingle().value1();
        var ids = db.select(FILES.ID).from(from).where(filter).orderBy(link.createdAt().desc(), FILES.ID)
                .limit(page.sizeOrDefault()).offset(page.offset()).fetch(FILES.ID);
        return PageResponse.of(ids.stream().map(file -> describe(ws, file)).toList(), page, total);
    }

    @Transactional
    public void attach(CurrentUser user, SourceType type, UUID id, UUID fileId, boolean remove) {
        UUID ws = workspaces.require(user).workspace().id();
        var target = access.require(ws, type, id, true);
        var link = attachment(type);
        var file = require(ws, fileId);
        access.requireProject(ws, file.getProjectId(), true);
        UUID projectId = type == SourceType.TASK ? target.get(TASKS.PROJECT_ID) : target.get(NOTES.PROJECT_ID);
        if (!Objects.equals(projectId, file.getProjectId())) throw conflict(ErrorCode.FILE_SCOPE_CONFLICT, "仅可关联同一项目范围的文件");
        if (remove) db.deleteFrom(link.table()).where(link.workspace().eq(ws), link.source().eq(id), link.file().eq(fileId)).execute();
        else {
            if (!file.getState().equals("STORED")) throw missing();
            db.insertInto(link.table()).set(link.workspace(), ws).set(link.source(), id).set(link.file(), fileId)
                    .set(link.createdBy(), user.userId()).onConflictDoNothing().execute();
        }
    }

    private AttachmentTable attachment(SourceType type) {
        return switch (type) {
            case TASK -> new AttachmentTable(TASK_FILES, TASK_FILES.WORKSPACE_ID, TASK_FILES.TASK_ID, TASK_FILES.FILE_ID, TASK_FILES.CREATED_AT, TASK_FILES.CREATED_BY);
            case NOTE -> new AttachmentTable(NOTE_FILES, NOTE_FILES.WORKSPACE_ID, NOTE_FILES.NOTE_ID, NOTE_FILES.FILE_ID, NOTE_FILES.CREATED_AT, NOTE_FILES.CREATED_BY);
            default -> throw invalid("资源不支持附件");
        };
    }
    private record AttachmentTable(Table<?> table, Field<UUID> workspace, Field<UUID> source, Field<UUID> file,
                                   Field<OffsetDateTime> createdAt, Field<UUID> createdBy) {}

    private boolean hasReferences(UUID ws, UUID id) {
        return db.fetchExists(TASK_FILES, TASK_FILES.WORKSPACE_ID.eq(ws).and(TASK_FILES.FILE_ID.eq(id)))
                || db.fetchExists(NOTE_FILES, NOTE_FILES.WORKSPACE_ID.eq(ws).and(NOTE_FILES.FILE_ID.eq(id)));
    }
    public FilesRecord require(UUID ws, UUID id) {
        var file = db.selectFrom(FILES).where(scope(ws, id), FILES.STATE.ne("PURGED")).fetchOne();
        if (file == null) throw missing();
        return file;
    }
    private FilesRecord writable(UUID ws, UUID id) {
        access.require(ws, SourceType.FILE, id, true);
        return require(ws, id);
    }
    private void checkVersion(FilesRecord file, long version) {
        if (file.getVersion() != version) throw conflict(ErrorCode.VERSION_CONFLICT, "文件已改变，请刷新后核对");
    }
    private Condition extractionScope(FilesRecord file) {
        return FILE_EXTRACTIONS.WORKSPACE_ID.eq(file.getWorkspaceId()).and(FILE_EXTRACTIONS.FILE_ID.eq(file.getId()))
                .and(FILE_EXTRACTIONS.CONTENT_VERSION.eq(file.getContentVersion())).and(FILE_EXTRACTIONS.EXTRACTION_GENERATION.eq(file.getExtractionGeneration()));
    }
    private Condition scope(UUID ws, UUID id) { return FILES.WORKSPACE_ID.eq(ws).and(FILES.ID.eq(id)); }
    private OffsetDateTime now() { return OffsetDateTime.now(ZoneOffset.UTC); }

    public static ObjectStorage.Ref ref(Record record) {
        if (record instanceof FilesRecord file) return new ObjectStorage.Ref(file.getStorageProfileId(), file.getStorageBucket(), file.getObjectKey(), file.getStorageVersionId());
        var attempt = record.into(FILE_UPLOAD_ATTEMPTS);
        return new ObjectStorage.Ref(attempt.getStorageProfileId(), attempt.getStorageBucket(), attempt.getObjectKey(), null);
    }
    public static AppException storageFailure(StorageException error) {
        if (error.code() == StorageException.Code.INTEGRITY_MISMATCH)
            return new AppException(ErrorCode.FILE_INTEGRITY_CHECK_FAILED, HttpStatus.BAD_GATEWAY, "文件完整性校验失败，请重新上传");
        return new AppException(error.code() == StorageException.Code.OBJECT_NOT_FOUND ? ErrorCode.FILE_CONTENT_UNAVAILABLE : ErrorCode.FILE_STORAGE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE, "文件存储暂时不可用，请稍后重试");
    }
}
