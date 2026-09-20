package com.example.workspace.note.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.note.application.NoteService;
import com.example.workspace.note.dto.CreateNoteRequest;
import com.example.workspace.note.dto.NoteResponse;
import com.example.workspace.note.dto.UpdateNoteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@Tag(name = "Notes")
@AuthenticatedApi
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建笔记", description = "可关联项目。未传标题时使用「无标题」。已归档项目不能新增笔记。")
    @ApiResponse(responseCode = "201", description = "已创建", content = @Content(schema = @Schema(implementation = NoteResponse.class)))
    @ApiResponse(responseCode = "404", description = "关联项目不存在")
    @ApiResponse(responseCode = "422", description = "已归档项目不可新增笔记")
    public NoteResponse create(CurrentUser currentUser, @Valid @RequestBody CreateNoteRequest request) {
        return noteService.create(currentUser, request);
    }

    @GetMapping
    @Operation(summary = "笔记列表", description = "列表不返回正文 `content`，只带 `summary`。默认排除已归档笔记。")
    @ApiResponse(responseCode = "200", description = "分页笔记列表")
    public PageResponse<NoteResponse> list(
            CurrentUser currentUser,
            @Parameter(description = "按所属项目筛选") @RequestParam(required = false) UUID projectId,
            @Parameter(description = "为 true 时只返回收藏笔记") @RequestParam(required = false) Boolean favorite,
            @Parameter(description = "为 true 时只返回已归档笔记，默认 false") @RequestParam(required = false) Boolean archived,
            @Parameter(description = "按标题或摘要关键词筛选") @RequestParam(required = false) String keyword,
            @Valid @ParameterObject PageQuery pageQuery
    ) {
        return noteService.list(currentUser, projectId, favorite, archived, keyword, pageQuery);
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "笔记详情", description = "详情包含完整 Markdown 正文。")
    @ApiResponse(responseCode = "200", description = "笔记详情", content = @Content(schema = @Schema(implementation = NoteResponse.class)))
    @ApiResponse(responseCode = "404", description = "笔记不存在或不在当前 Workspace")
    public NoteResponse get(
            CurrentUser currentUser,
            @Parameter(description = "笔记 ID", required = true) @PathVariable UUID noteId
    ) {
        return noteService.get(currentUser, noteId);
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "可改标题、正文、收藏和归档状态。已归档项目下的笔记只读。")
    @ApiResponse(responseCode = "200", description = "更新后的笔记", content = @Content(schema = @Schema(implementation = NoteResponse.class)))
    @ApiResponse(responseCode = "404", description = "笔记或关联项目不存在")
    @ApiResponse(responseCode = "409", description = "笔记已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "已归档项目只读")
    public NoteResponse update(
            CurrentUser currentUser,
            @Parameter(description = "笔记 ID", required = true) @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        return noteService.update(currentUser, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除笔记", description = "软删除笔记。")
    @ApiResponse(responseCode = "204", description = "已删除", content = @Content)
    @ApiResponse(responseCode = "404", description = "笔记不存在或不在当前 Workspace")
    public void delete(
            CurrentUser currentUser,
            @Parameter(description = "笔记 ID", required = true) @PathVariable UUID noteId
    ) {
        noteService.delete(currentUser, noteId);
    }
}
