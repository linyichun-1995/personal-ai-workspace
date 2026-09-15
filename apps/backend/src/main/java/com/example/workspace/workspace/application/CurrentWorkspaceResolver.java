package com.example.workspace.workspace.application;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.workspace.domain.Workspace;
import org.springframework.stereotype.Component;

@Component
public class CurrentWorkspaceResolver {

    private final WorkspaceService workspaceService;
    private final WorkspaceAccessService workspaceAccessService;

    public CurrentWorkspaceResolver(
            WorkspaceService workspaceService,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.workspaceService = workspaceService;
        this.workspaceAccessService = workspaceAccessService;
    }

    public WorkspaceAccess require(CurrentUser currentUser) {
        Workspace workspace = workspaceService.requireDefaultForUser(currentUser.userId());
        return workspaceAccessService.requireMember(currentUser.userId(), workspace.id());
    }
}
