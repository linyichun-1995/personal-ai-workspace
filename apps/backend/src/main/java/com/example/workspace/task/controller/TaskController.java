package com.example.workspace.task.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.task.application.TaskService;
import com.example.workspace.task.domain.TaskDueFilter;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.dto.CreateTaskRequest;
import com.example.workspace.task.dto.TaskResponse;
import com.example.workspace.task.dto.UpdateTaskRequest;
import com.example.workspace.task.dto.UpdateTaskStatusRequest;
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
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks")
@AuthenticatedApi
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建任务", description = "可挂到项目下，也可创建一级子任务。V0.1 不支持多级嵌套。已归档项目不能新增任务。")
    @ApiResponse(responseCode = "201", description = "已创建", content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "404", description = "关联的项目或父任务不存在")
    @ApiResponse(responseCode = "422", description = "已归档项目、多级子任务、父子项目不一致、时间不合法或状态无效")
    public TaskResponse create(CurrentUser currentUser, @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(currentUser, request);
    }

    @GetMapping
    @Operation(summary = "任务列表", description = "可按项目、状态、优先级和到期筛选。默认按到期时间升序，空截止日期排在后面。")
    @ApiResponse(responseCode = "200", description = "分页任务列表")
    public PageResponse<TaskResponse> list(
            CurrentUser currentUser,
            @Parameter(description = "按所属项目筛选") @RequestParam(required = false) UUID projectId,
            @Parameter(
                    description = "按状态筛选，多个值用逗号分隔",
                    example = "TODO,IN_PROGRESS",
                    schema = @Schema(allowableValues = {"TODO", "IN_PROGRESS", "DONE", "CANCELLED"})
            )
            @RequestParam(required = false) String status,
            @Parameter(description = "按优先级筛选") @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "到期筛选：今天、已逾期或即将到期") @RequestParam(required = false) TaskDueFilter due,
            @Valid @ParameterObject PageQuery pageQuery
    ) {
        return taskService.list(currentUser, projectId, status, priority, due, pageQuery);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "任务详情")
    @ApiResponse(responseCode = "200", description = "任务详情", content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "404", description = "任务不存在或不在当前 Workspace")
    public TaskResponse get(
            CurrentUser currentUser,
            @Parameter(description = "任务 ID", required = true) @PathVariable UUID taskId
    ) {
        return taskService.get(currentUser, taskId);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "更新任务", description = "全量更新任务字段。已归档项目下的任务只读。")
    @ApiResponse(responseCode = "200", description = "更新后的任务", content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "404", description = "任务、项目或父任务不存在")
    @ApiResponse(responseCode = "409", description = "任务已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "已归档只读、父子关系非法、时间不合法或状态无效")
    public TaskResponse update(
            CurrentUser currentUser,
            @Parameter(description = "任务 ID", required = true) @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(currentUser, taskId, request);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "更新任务状态", description = "进入 `DONE` 时会记录完成时间。")
    @ApiResponse(responseCode = "200", description = "更新后的任务", content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "404", description = "任务不存在或不在当前 Workspace")
    @ApiResponse(responseCode = "409", description = "任务已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "已归档只读或状态无效")
    public TaskResponse updateStatus(
            CurrentUser currentUser,
            @Parameter(description = "任务 ID", required = true) @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskService.updateStatus(currentUser, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除任务", description = "软删除任务。")
    @ApiResponse(responseCode = "204", description = "已删除", content = @Content)
    @ApiResponse(responseCode = "404", description = "任务不存在或不在当前 Workspace")
    public void delete(
            CurrentUser currentUser,
            @Parameter(description = "任务 ID", required = true) @PathVariable UUID taskId
    ) {
        taskService.delete(currentUser, taskId);
    }
}
