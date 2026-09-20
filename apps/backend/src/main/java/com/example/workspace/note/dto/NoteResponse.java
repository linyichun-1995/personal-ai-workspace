package com.example.workspace.note.dto;

import com.example.workspace.note.domain.Note;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "NoteResponse", description = "笔记。列表接口的 `content` 为 null，详情接口返回完整正文。")
public record NoteResponse(
        @Schema(description = "笔记 ID") UUID id,
        @Schema(description = "所属工作空间 ID") UUID workspaceId,
        @Schema(description = "关联项目 ID，可空") UUID projectId,
        @Schema(description = "标题") String title,
        @Schema(description = "Markdown 正文；列表中为 null") String content,
        @Schema(description = "正文摘要") String summary,
        @Schema(description = "是否收藏") boolean favorite,
        @Schema(description = "是否归档") boolean archived,
        @Schema(description = "创建时间") Instant createdAt,
        @Schema(description = "更新时间") Instant updatedAt,
        @Schema(description = "乐观锁版本") long version
) {
    public static NoteResponse from(Note note, boolean includeContent) {
        return new NoteResponse(
                note.id(),
                note.workspaceId(),
                note.projectId(),
                note.title(),
                includeContent ? note.content() : null,
                note.summary(),
                note.favorite(),
                note.archived(),
                note.createdAt(),
                note.updatedAt(),
                note.version()
        );
    }
}
