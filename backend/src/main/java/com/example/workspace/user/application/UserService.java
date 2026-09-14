package com.example.workspace.user.application;

import com.example.workspace.common.exception.ResourceNotFoundException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.user.domain.User;
import com.example.workspace.user.dto.CurrentUserResponse;
import com.example.workspace.user.dto.WorkspaceSummaryResponse;
import com.example.workspace.user.repository.UserRepository;
import com.example.workspace.workspace.application.WorkspaceService;
import com.example.workspace.workspace.domain.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;

    public UserService(UserRepository userRepository, WorkspaceService workspaceService) {
        this.userRepository = userRepository;
        this.workspaceService = workspaceService;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Workspace workspace = workspaceService.requireById(currentUser.workspaceId());
        return new CurrentUserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.locale(),
                user.timezone(),
                new WorkspaceSummaryResponse(workspace.id(), workspace.name(), workspace.slug(), workspace.timezone())
        );
    }
}
