package com.example.workspace.workspace.controller;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.workspace.application.WorkspaceAccess;
import com.example.workspace.workspace.application.WorkspaceAccessService;
import com.example.workspace.workspace.application.WorkspaceService;
import com.example.workspace.workspace.domain.Workspace;
import com.example.workspace.workspace.dto.UpdateWorkspaceRequest;
import com.example.workspace.workspace.dto.WorkspaceDetailResponse;
import com.example.workspace.workspace.dto.WorkspaceListResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceAccessService workspaceAccessService;
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceAccessService workspaceAccessService, WorkspaceService workspaceService) {
        this.workspaceAccessService = workspaceAccessService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public WorkspaceListResponse list(CurrentUser currentUser) {
        return new WorkspaceListResponse(
                workspaceAccessService.listAccessible(currentUser.userId()).stream()
                        .map(WorkspaceDetailResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceDetailResponse get(CurrentUser currentUser, @PathVariable UUID workspaceId) {
        return WorkspaceDetailResponse.from(workspaceAccessService.requireMember(currentUser.userId(), workspaceId));
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceDetailResponse update(
            CurrentUser currentUser,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(currentUser.userId(), workspaceId);
        Workspace updated = workspaceService.updateSettings(access, request);
        return WorkspaceDetailResponse.from(new WorkspaceAccess(updated, access.member()));
    }
}
