package com.example.workspace.project.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.project.application.ProjectService;
import com.example.workspace.project.domain.ProjectPriority;
import com.example.workspace.project.dto.ArchiveProjectRequest;
import com.example.workspace.project.dto.CreateProjectRequest;
import com.example.workspace.project.dto.ProjectResponse;
import com.example.workspace.project.dto.UpdateProjectRequest;
import com.example.workspace.project.dto.UpdateProjectStatusRequest;
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
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(CurrentUser currentUser, @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(currentUser, request);
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            CurrentUser currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) ProjectPriority priority,
            @RequestParam(required = false) Boolean archived,
            @Valid PageQuery pageQuery
    ) {
        return projectService.list(currentUser, status, priority, archived, pageQuery);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(CurrentUser currentUser, @PathVariable UUID projectId) {
        return projectService.get(currentUser, projectId);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(
            CurrentUser currentUser,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.update(currentUser, projectId, request);
    }

    @PatchMapping("/{projectId}/status")
    public ProjectResponse updateStatus(
            CurrentUser currentUser,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectStatusRequest request
    ) {
        return projectService.updateStatus(currentUser, projectId, request);
    }

    @PostMapping("/{projectId}/archive")
    public ProjectResponse archive(
            CurrentUser currentUser,
            @PathVariable UUID projectId,
            @Valid @RequestBody ArchiveProjectRequest request
    ) {
        return projectService.archive(currentUser, projectId, request);
    }

    @PostMapping("/{projectId}/restore")
    public ProjectResponse restore(
            CurrentUser currentUser,
            @PathVariable UUID projectId,
            @Valid @RequestBody ArchiveProjectRequest request
    ) {
        return projectService.restore(currentUser, projectId, request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser currentUser, @PathVariable UUID projectId) {
        projectService.delete(currentUser, projectId);
    }
}
