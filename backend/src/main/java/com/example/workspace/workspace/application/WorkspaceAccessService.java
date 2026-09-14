package com.example.workspace.workspace.application;

import com.example.workspace.common.exception.ForbiddenException;
import com.example.workspace.common.exception.ResourceNotFoundException;
import com.example.workspace.workspace.domain.WorkspaceMember;
import com.example.workspace.workspace.repository.WorkspaceMemberRepository;
import com.example.workspace.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAccessService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceAccessService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public WorkspaceAccess requireMember(UUID userId, UUID workspaceId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(ResourceNotFoundException::workspace);
        return workspaceRepository.findById(workspaceId)
                .map(workspace -> new WorkspaceAccess(workspace, member))
                .orElseThrow(ResourceNotFoundException::workspace);
    }

    public WorkspaceAccess requireOwner(UUID userId, UUID workspaceId) {
        WorkspaceAccess access = requireMember(userId, workspaceId);
        if (!access.isOwner()) {
            throw ForbiddenException.workspaceOwnerRequired();
        }
        return access;
    }

    public List<WorkspaceAccess> listAccessible(UUID userId) {
        return workspaceMemberRepository.findAllByUserId(userId).stream()
                .map(member -> workspaceRepository.findById(member.workspaceId())
                        .map(workspace -> new WorkspaceAccess(workspace, member))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}
