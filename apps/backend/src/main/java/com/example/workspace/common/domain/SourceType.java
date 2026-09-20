package com.example.workspace.common.domain;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.exception.AppException;
import org.springframework.http.HttpStatus;

public enum SourceType {
    PROJECT("projects", "project_tags", "name"),
    TASK("tasks", "task_tags", "title"),
    NOTE("notes", "note_tags", "title"),
    FILE("files", "file_tags", "display_name");
    public final String table;
    public final String tagTable;
    public final String titleColumn;
    SourceType(String table, String tagTable, String titleColumn) {
        this.table = table; this.tagTable = tagTable; this.titleColumn = titleColumn;
    }
    public static SourceType fromResource(String resource) {
        for (var type : values()) if (type.table.equals(resource)) return type;
        throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "资源不存在");
    }
}
