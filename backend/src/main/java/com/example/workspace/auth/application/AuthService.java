package com.example.workspace.auth.application;

import com.example.workspace.auth.dto.AuthResponse;
import com.example.workspace.auth.dto.IssuedTokens;
import com.example.workspace.auth.dto.LoginRequest;
import com.example.workspace.auth.dto.RegisterRequest;
import com.example.workspace.common.exception.ConflictException;
import com.example.workspace.common.exception.UnauthorizedException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.Emails;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.infrastructure.redis.RefreshSession;
import com.example.workspace.infrastructure.redis.RefreshTokenStore;
import com.example.workspace.user.domain.User;
import com.example.workspace.user.domain.UserStatus;
import com.example.workspace.user.dto.UserResponse;
import com.example.workspace.user.dto.WorkspaceResponse;
import com.example.workspace.user.repository.UserRepository;
import com.example.workspace.workspace.application.WorkspaceService;
import com.example.workspace.workspace.domain.Workspace;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;
    private final WorkspaceService workspaceService;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenStore refreshTokenStore,
            WorkspaceService workspaceService,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer
    ) {
        this.userRepository = userRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.workspaceService = workspaceService;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String emailNormalized = Emails.normalize(request.email());
        if (userRepository.existsByNormalizedEmail(emailNormalized)) {
            throw ConflictException.emailTaken();
        }

        Instant now = Instant.now();
        String name = request.name().trim();

        User user = new User(
                UuidV7.next(),
                emailNormalized,
                emailNormalized,
                passwordEncoder.encode(request.password()),
                name,
                null,
                "zh-CN",
                "UTC",
                UserStatus.ACTIVE,
                now,
                now,
                null,
                0
        );

        try {
            userRepository.insert(user);
        }
        catch (DataIntegrityViolationException exception) {
            throw ConflictException.emailTaken();
        }

        Workspace workspace = workspaceService.createPersonalWorkspace(user.id(), user.timezone(), now);
        log.info("Registered user {} with workspace {}", user.id(), workspace.id());
        return toSession(user, workspace, tokenIssuer.issue(new CurrentUser(user.id(), user.email())));
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String emailNormalized = Emails.normalize(request.email());
        User user = userRepository.findByNormalizedEmail(emailNormalized)
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw UnauthorizedException.invalidCredentials();
        }
        if (user.status() != UserStatus.ACTIVE) {
            throw UnauthorizedException.accountDisabled();
        }

        Instant now = Instant.now();
        userRepository.updateLastLoginAt(user.id(), now);
        Workspace workspace = workspaceService.requireDefaultForUser(user.id());
        log.info("User {} logged in", user.id());
        return toSession(user, workspace, tokenIssuer.issue(new CurrentUser(user.id(), user.email())));
    }

    public AuthSession refresh(String rawRefreshToken) {
        RefreshSession stored = refreshTokenStore.find(rawRefreshToken)
                .orElseThrow(UnauthorizedException::refreshTokenInvalid);

        if (stored.revoked()) {
            tokenIssuer.revokeFamily(stored.familyId());
            throw UnauthorizedException.refreshTokenInvalid();
        }
        if (stored.expired(Instant.now())) {
            throw UnauthorizedException.tokenExpired();
        }

        User user = userRepository.findById(stored.userId())
                .orElseThrow(UnauthorizedException::refreshTokenInvalid);
        if (user.status() != UserStatus.ACTIVE) {
            tokenIssuer.revokeFamily(stored.familyId());
            throw UnauthorizedException.accountDisabled();
        }

        Workspace workspace = workspaceService.requireDefaultForUser(user.id());
        IssuedTokens tokens = tokenIssuer.rotate(
                new CurrentUser(user.id(), user.email()),
                stored.familyId(),
                rawRefreshToken
        );
        return toSession(user, workspace, tokens);
    }

    public void logout(String rawRefreshToken) {
        tokenIssuer.revoke(rawRefreshToken);
    }

    private static AuthSession toSession(User user, Workspace workspace, IssuedTokens tokens) {
        return new AuthSession(
                AuthResponse.of(
                        new UserResponse(user.id(), user.email(), user.displayName(), user.avatarUrl()),
                        new WorkspaceResponse(workspace.id(), workspace.name()),
                        tokens
                ),
                tokens.refreshToken()
        );
    }
}
