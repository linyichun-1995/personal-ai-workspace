package com.example.workspace.common.domain;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.exception.AppException;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SourceAccess {
    private final DSLContext db;
    public SourceAccess(DSLContext db) { this.db = db; }
    public Record require(UUID workspace, SourceType type, UUID id, boolean writable) {
        var source = SourceTables.of(type);
        var query = db.selectFrom(source.table()).where(source.identity(workspace, id));
        var row = writable ? query.forUpdate().fetchOne() : query.fetchOne();
        if (row == null || row.get(source.deletedAt()) != null || (type == SourceType.FILE && !"STORED".equals(row.get(FILES.STATE))))
            throw missing();
        UUID project = row.get(source.projectId());
        requireProject(workspace, project, writable);
        if (writable && type == SourceType.NOTE && Boolean.TRUE.equals(row.get(NOTES.ARCHIVED)))
            throw conflict(ErrorCode.RESOURCE_READ_ONLY, "已归档笔记为只读");
        return row;
    }
    public Record requireProject(UUID workspace, UUID id, boolean writable) {
        if (id == null) return null;
        var query = db.selectFrom(PROJECTS).where(PROJECTS.WORKSPACE_ID.eq(workspace)
                .and(PROJECTS.ID.eq(id)).and(PROJECTS.DELETED_AT.isNull()));
        var row = writable ? query.forUpdate().fetchOne() : query.fetchOne();
        if (row == null) throw missing();
        if (writable && row.get(PROJECTS.ARCHIVED_AT) != null) throw conflict(ErrorCode.PROJECT_ARCHIVED, "已归档项目为只读，请先恢复");
        return row;
    }
    public static AppException missing() { return new AppException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "资源不存在"); }
    public static AppException conflict(ErrorCode code, String message) { return new AppException(code, HttpStatus.CONFLICT, message); }
    public static AppException invalid(String message) { return new AppException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message); }
}
