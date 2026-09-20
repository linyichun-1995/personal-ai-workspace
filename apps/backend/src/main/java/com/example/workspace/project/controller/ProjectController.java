package com.example.workspace.project.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.project.application.ProjectService;
import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.dto.ArchiveProjectRequest;
import com.example.workspace.project.dto.CreateProjectRequest;
import com.example.workspace.project.dto.ProjectResponse;
import com.example.workspace.project.dto.UpdateProjectRequest;
import com.example.workspace.project.dto.UpdateProjectStatusRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects")
@AuthenticatedApi
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建项目", description = "在当前默认 Workspace 下创建项目。未传 `status` / `priority` 时使用服务端默认值。")
    @ApiResponse(responseCode = "201", description = "已创建", content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "422", description = "截止日期早于开始日期，或状态无效")
    public ProjectResponse create(CurrentUser currentUser, @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(currentUser, request);
    }

    @GetMapping
    @Operation(summary = "项目列表", description = "默认排除已归档项目。`archived=true` 只返回已归档项。列表项包含任务/笔记统计。")
    @ApiResponse(responseCode = "200", description = "分页项目列表")
    public PageResponse<ProjectResponse> list(
            CurrentUser currentUser,
            @Parameter(
                    description = "按状态筛选，多个值用逗号分隔",
                    example = "ACTIVE,PLANNED",
                    schema = @Schema(allowableValues = {"PLANNED", "ACTIVE", "PAUSED", "COMPLETED"})
            )
            @RequestParam(required = false) String status,
            @Parameter(description = "按优先级筛选") @RequestParam(required = false) ProjectPriority priority,
            @Parameter(description = "为 true 时只返回已归档项目，默认 false") @RequestParam(required = false) Boolean archived,
            @Valid @ParameterObject PageQuery pageQuery
    ) {
        return projectService.list(currentUser, status, priority, archived, pageQuery);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "项目详情")
    @ApiResponse(responseCode = "200", description = "项目详情", content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "404", description = "项目不存在或不在当前 Workspace")
    public ProjectResponse get(
            CurrentUser currentUser,
            @Parameter(description = "项目 ID", required = true) @PathVariable UUID projectId
    ) {
        return projectService.get(currentUser, projectId);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "更新项目", description = "全量更新项目字段。已归档项目为只读，需先恢复。")
    @ApiResponse(responseCode = "200", description = "更新后的项目", content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "404", description = "项目不存在或不在当前 Workspace")
    @ApiResponse(responseCode = "409", description = "项目已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "已归档只读、日期不合法或状态无效")
    public ProjectResponse update(
            CurrentUser currentUser,
            @Parameter(description = "项目 ID", required = true) @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.update(currentUser, projectId, request);
    }

    @PatchMapping("/{projectId}/status")
    @Operation(summary = "更新项目状态")
    @ApiResponse(responseCode = "200", description = "更新后的项目", content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "404", description = "项目不存在或不在当前 Workspace")
    @ApiResponse(responseCode = "409", description = "项目已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "已归档只读或状态无效")
    public ProjectResponse updateStatus(
            CurrentUser currentUser,
            @Parameter(description = "项目 ID", required = true) @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectStatusRequest request
    ) {
        return projectService.updateStatus(currentUser, projectId, request);
    }

    @PostMapping("/{projectId}/archive")
    @Operation(summary = "归档项目", description = "归档后项目只读，不能再新增或编辑任务/笔记。")
    @ApiResponse(responseCode = "200", description = "已归档的项目", content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "404", description = "项目不存在或不在当前 Workspace")
    @ApiResponse(responseCode = "409", description = "项目已被其他请求更新")
    public ProjectResponse archive(
            CurrentUser currentUser,
            @Parameter(description = "项目 ID", required = true) @PathVariable UUID projectId,
            @Valid @RequestBody ArchiveProjectRequest request
    ) {
        return projectService.archive(currentUser, projectId, request);
    }

    @PostMapping("/{projectId}/restore")
    @Operation(summary = "恢复项目", description = "取消归档，恢复可编辑。")
    @ApiResponse(responseCode = "200", description = "已恢复的项目", content = @Content(schema = @Schema(implementation = ProjectResponse.class)))
    @ApiResponse(responseCode = "404", description = "项目不存在或不在当前 Workspace")
    @ApiResponse(responseCode = "409", description = "项目已被其他请求更新")
    public ProjectResponse restore(
            CurrentUser currentUser,
            @Parameter(description = "项目 ID", required = true) @PathVariable UUID projectId,
            @Valid @RequestBody ArchiveProjectRequest request
    ) {
        return projectService.restore(currentUser, projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除项目", description = "软删除项目，并级联软删除其任务和笔记。")
    @ApiResponse(responseCode = "204", description = "已删除", content = @Content)
    @ApiResponse(responseCode = "404", description = "项目不存在或不在当前 Workspace")
    public void delete(
            CurrentUser currentUser,
            @Parameter(description = "项目 ID", required = true) @PathVariable UUID projectId
    ) {
        projectService.delete(currentUser, projectId);
    }
}
