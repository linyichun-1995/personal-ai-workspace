package com.example.workspace.workspace.application;

import com.example.workspace.workspace.domain.Workspace;
import com.example.workspace.workspace.domain.WorkspaceMember;
import com.example.workspace.workspace.domain.WorkspaceRole;

public record WorkspaceAccess(
        Workspace workspace,
        WorkspaceMember member
) {
    public boolean isOwner() {
        return member.role() == WorkspaceRole.OWNER;
    }
}
