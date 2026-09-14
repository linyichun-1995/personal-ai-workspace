package com.example.workspace.user.application;

import com.example.workspace.auth.application.AuthSession;
import com.example.workspace.auth.application.TokenIssuer;
import com.example.workspace.auth.dto.AuthResponse;
import com.example.workspace.auth.dto.IssuedTokens;
import com.example.workspace.common.exception.BusinessException;
import com.example.workspace.common.exception.ResourceNotFoundException;
import com.example.workspace.common.exception.UnauthorizedException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.AvatarUrls;
import com.example.workspace.common.util.TimeZones;
import com.example.workspace.infrastructure.redis.RefreshTokenStore;
import com.example.workspace.infrastructure.redis.TokenEpochStore;
import com.example.workspace.user.domain.User;
import com.example.workspace.user.dto.ChangePasswordRequest;
import com.example.workspace.user.dto.CurrentUserResponse;
import com.example.workspace.user.dto.UpdateProfileRequest;
import com.example.workspace.user.dto.UserResponse;
import com.example.workspace.user.dto.WorkspaceResponse;
import com.example.workspace.user.repository.UserRepository;
import com.example.workspace.workspace.application.WorkspaceService;
import com.example.workspace.workspace.domain.Workspace;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final PasswordEncoder passwordEncoder;
    private final TokenEpochStore tokenEpochStore;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenIssuer tokenIssuer;

    public UserService(
            UserRepository userRepository,
            WorkspaceService workspaceService,
            PasswordEncoder passwordEncoder,
            TokenEpochStore tokenEpochStore,
            RefreshTokenStore refreshTokenStore,
            TokenIssuer tokenIssuer
    ) {
        this.userRepository = userRepository;
        this.workspaceService = workspaceService;
        this.passwordEncoder = passwordEncoder;
        this.tokenEpochStore = tokenEpochStore;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me(CurrentUser currentUser) {
        User user = requireUser(currentUser);
        Workspace workspace = workspaceService.requireDefaultForUser(currentUser.userId());
        return toCurrentUserResponse(user, workspace);
    }

    @Transactional
    public CurrentUserResponse updateProfile(CurrentUser currentUser, UpdateProfileRequest request) {
        requireUser(currentUser);
        User updated = userRepository.updateProfile(
                currentUser.userId(),
                request.version(),
                request.displayName().trim(),
                AvatarUrls.normalize(request.avatarUrl()),
                request.locale().trim(),
                TimeZones.requireValid(request.timezone()),
                Instant.now()
        );
        Workspace workspace = workspaceService.requireDefaultForUser(currentUser.userId());
        return toCurrentUserResponse(updated, workspace);
    }

    @Transactional
    public AuthSession changePassword(CurrentUser currentUser, ChangePasswordRequest request) {
        User user = requireUser(currentUser);
        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw UnauthorizedException.invalidCredentials();
        }
        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }

        User updated = userRepository.updatePassword(
                user.id(),
                passwordEncoder.encode(request.newPassword()),
                Instant.now()
        );
        tokenEpochStore.increment(user.id());
        refreshTokenStore.revokeAllForUser(user.id());

        Workspace workspace = workspaceService.requireDefaultForUser(user.id());
        IssuedTokens tokens = tokenIssuer.issue(new CurrentUser(updated.id(), updated.email()));
        return new AuthSession(
                AuthResponse.of(
                        new UserResponse(updated.id(), updated.email(), updated.displayName(), updated.avatarUrl()),
                        new WorkspaceResponse(workspace.id(), workspace.name()),
                        tokens
                ),
                tokens.refreshToken()
        );
    }

    private User requireUser(CurrentUser currentUser) {
        return userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static CurrentUserResponse toCurrentUserResponse(User user, Workspace workspace) {
        return new CurrentUserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.avatarUrl(),
                user.locale(),
                user.timezone(),
                user.version(),
                new WorkspaceResponse(workspace.id(), workspace.name())
        );
    }
}
