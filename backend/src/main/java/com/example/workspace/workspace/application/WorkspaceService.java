package com.example.workspace.workspace.application;

import com.example.workspace.common.util.TimeZones;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.workspace.domain.Workspace;
import com.example.workspace.workspace.domain.WorkspaceMember;
import com.example.workspace.workspace.domain.WorkspaceRole;
import com.example.workspace.workspace.domain.WorkspaceType;
import com.example.workspace.workspace.dto.UpdateWorkspaceRequest;
import com.example.workspace.workspace.repository.WorkspaceMemberRepository;
import com.example.workspace.workspace.repository.WorkspaceRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    public static final String DEFAULT_PERSONAL_WORKSPACE_NAME = "我的工作空间";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public Workspace createPersonalWorkspace(UUID userId, String timezone, Instant now) {
        UUID workspaceId = UuidV7.next();
        Workspace workspace = new Workspace(
                workspaceId,
                DEFAULT_PERSONAL_WORKSPACE_NAME,
                "personal-" + workspaceId.toString().replace("-", "").substring(0, 12),
                WorkspaceType.PERSONAL,
                timezone,
                1,
                now,
                now,
                0
        );
        workspaceRepository.insert(workspace);
        workspaceMemberRepository.insert(new WorkspaceMember(
                UuidV7.next(),
                workspaceId,
                userId,
                WorkspaceRole.OWNER,
                now
        ));
        return workspace;
    }

    public Workspace requireById(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalStateException("Workspace not found: " + workspaceId));
    }

    public Workspace requireDefaultForUser(UUID userId) {
        return workspaceMemberRepository.findByUserId(userId)
                .flatMap(member -> workspaceRepository.findById(member.workspaceId()))
                .orElseThrow(() -> new IllegalStateException("User has no workspace: " + userId));
    }

    @Transactional
    public Workspace updateSettings(WorkspaceAccess access, UpdateWorkspaceRequest request) {
        String name = request.name().trim();
        String timezone = TimeZones.requireValid(request.timezone());
        return workspaceRepository.updateSettings(
                access.workspace().id(),
                request.version(),
                name,
                timezone,
                request.weekStartsOn(),
                Instant.now()
        );
    }
}
