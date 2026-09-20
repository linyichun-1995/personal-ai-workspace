package com.example.workspace.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(name = "UpdateNoteRequest", description = "更新笔记")
public record UpdateNoteRequest(
        @Schema(description = "关联项目，可空") UUID projectId,
        @Schema(description = "标题")
        @Size(max = 300) String title,
        @Schema(description = "Markdown 正文")
        @Size(max = 200_000) String content,
        @Schema(description = "是否收藏，省略则保持原值") Boolean favorite,
        @Schema(description = "是否归档笔记本身，省略则保持原值") Boolean archived,
        @Schema(description = "当前笔记版本")
        @NotNull Long version
) {
}
