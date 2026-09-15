package com.example.workspace.task.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.task.application.TaskService;
import com.example.workspace.task.domain.TaskDueFilter;
import com.example.workspace.task.domain.TaskPriority;
import com.example.workspace.task.dto.CreateTaskRequest;
import com.example.workspace.task.dto.TaskResponse;
import com.example.workspace.task.dto.UpdateTaskRequest;
import com.example.workspace.task.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import java.util.UUID;
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
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(CurrentUser currentUser, @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(currentUser, request);
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            CurrentUser currentUser,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) TaskDueFilter due,
            @Valid PageQuery pageQuery
    ) {
        return taskService.list(currentUser, projectId, status, priority, due, pageQuery);
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(CurrentUser currentUser, @PathVariable UUID taskId) {
        return taskService.get(currentUser, taskId);
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(
            CurrentUser currentUser,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(currentUser, taskId, request);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse updateStatus(
            CurrentUser currentUser,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskService.updateStatus(currentUser, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser currentUser, @PathVariable UUID taskId) {
        taskService.delete(currentUser, taskId);
    }
}
