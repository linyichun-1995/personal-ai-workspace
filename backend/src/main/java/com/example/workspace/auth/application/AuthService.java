package com.example.workspace.auth.application;

import com.example.workspace.auth.dto.LoginRequest;
import com.example.workspace.auth.dto.RegisterRequest;
import com.example.workspace.auth.dto.TokenResponse;
import com.example.workspace.common.exception.ConflictException;
import com.example.workspace.common.exception.UnauthorizedException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.Emails;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.infrastructure.redis.RefreshSession;
import com.example.workspace.infrastructure.redis.RefreshTokenStore;
import com.example.workspace.user.domain.User;
import com.example.workspace.user.domain.UserStatus;
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
    private final WorkspaceService workspaceService;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(
            UserRepository userRepository,
            WorkspaceService workspaceService,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer,
            RefreshTokenStore refreshTokenStore
    ) {
        this.userRepository = userRepository;
        this.workspaceService = workspaceService;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String emailNormalized = Emails.normalize(request.email());
        if (userRepository.existsByNormalizedEmail(emailNormalized)) {
            throw ConflictException.emailTaken();
        }

        Instant now = Instant.now();
        String displayName = resolveDisplayName(request, emailNormalized);
        String locale = request.locale() == null || request.locale().isBlank() ? "zh-CN" : request.locale();
        String timezone = request.timezone() == null || request.timezone().isBlank() ? "UTC" : request.timezone();

        User user = new User(
                UuidV7.next(),
                request.email().trim(),
                emailNormalized,
                passwordEncoder.encode(request.password()),
                displayName,
                null,
                locale,
                timezone,
                UserStatus.ACTIVE,
                now,
                now,
                0
        );

        try {
            userRepository.insert(user);
        }
        catch (DataIntegrityViolationException exception) {
            throw ConflictException.emailTaken();
        }

        Workspace workspace = workspaceService.createPersonalWorkspace(user.id(), displayName, timezone, now);
        log.info("Registered user {} with workspace {}", user.id(), workspace.id());
        return tokenIssuer.issue(new CurrentUser(user.id(), workspace.id(), user.email()));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String emailNormalized = Emails.normalize(request.email());
        User user = userRepository.findByNormalizedEmail(emailNormalized)
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw UnauthorizedException.invalidCredentials();
        }
        if (user.status() != UserStatus.ACTIVE) {
            throw UnauthorizedException.accountDisabled();
        }

        Workspace workspace = workspaceService.requireDefaultForUser(user.id());
        return tokenIssuer.issue(new CurrentUser(user.id(), workspace.id(), user.email()));
    }

    public TokenResponse refresh(String refreshToken) {
        RefreshSession session = refreshTokenStore.find(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));
        return tokenIssuer.rotate(
                new CurrentUser(session.userId(), session.workspaceId(), session.email()),
                session.familyId(),
                refreshToken
        );
    }

    public void logout(String refreshToken) {
        tokenIssuer.revoke(refreshToken);
    }

    private static String resolveDisplayName(RegisterRequest request, String emailNormalized) {
        if (request.displayName() != null && !request.displayName().isBlank()) {
            return request.displayName().trim();
        }
        int at = emailNormalized.indexOf('@');
        return at > 0 ? emailNormalized.substring(0, at) : emailNormalized;
    }
}
