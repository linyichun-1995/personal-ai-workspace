package com.example.workspace.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(name = "CreateNoteRequest", description = "创建笔记")
public record CreateNoteRequest(
        @Schema(description = "关联项目，可空") UUID projectId,
        @Schema(description = "标题，可空")
        @Size(max = 300) String title,
        @Schema(description = "Markdown 正文")
        @Size(max = 200_000) String content
) {
}
