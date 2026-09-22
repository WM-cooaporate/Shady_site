package com.shady.landing.auth;

import com.shady.landing.audit.AuditAction;
import com.shady.landing.audit.AuditService;
import com.shady.landing.auth.RefreshTokenService.IssuedRefreshToken;
import com.shady.landing.auth.RefreshTokenService.Rotation;
import com.shady.landing.common.error.BadRequestException;
import com.shady.landing.common.error.UnauthorizedException;
import com.shady.landing.security.AccessTokenService;
import com.shady.landing.security.AccessTokenService.IssuedAccessToken;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Admin login, token refresh, logout and password change.
 *
 * <p>{@link #login} is deliberately not transactional: failed attempts and their audit entries are committed
 * by their own transactions, so throwing the (generic) login error does not roll them back.
 */
@Service
public class AuthService {

    static final String LOGIN_FAILED = "Invalid email or password";
    static final String SESSION_EXPIRED = "Session expired. Please sign in again.";
    private static final String ENTITY = "ADMIN_USER";

    private final AdminUserRepository users;
    private final LoginAttemptService loginAttempts;
    private final RefreshTokenService refreshTokens;
    private final AccessTokenService accessTokens;
    private final AdminCredentialsChangeCheck credentialsCheck;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final Clock clock;
    /** Compared against when the user is unknown or locked, so response time does not reveal which. */
    private final String dummyHash;

    public AuthService(AdminUserRepository users, LoginAttemptService loginAttempts, RefreshTokenService refreshTokens,
                       AccessTokenService accessTokens, AdminCredentialsChangeCheck credentialsCheck,
                       PasswordEncoder passwordEncoder, AuditService audit, Clock clock) {
        this.users = users;
        this.loginAttempts = loginAttempts;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.credentialsCheck = credentialsCheck;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResult login(String email, String password, String ipAddress) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Optional<AdminUser> found = users.findByEmail(normalizedEmail);
        if (found.isEmpty()) {
            passwordMatches(password, dummyHash);
            throw loginFailed(normalizedEmail, "unknown_email");
        }
        AdminUser user = found.get();
        if (user.isLocked(Instant.now(clock))) {
            passwordMatches(password, dummyHash);
            throw loginFailed(normalizedEmail, "account_locked");
        }
        if (!passwordMatches(password, user.getPasswordHash())) {
            boolean lockedNow = loginAttempts.recordFailure(user.getId(), ipAddress);
            if (lockedNow) {
                audit.record(AuditAction.ACCOUNT_LOCKED, user.getEmail(), ENTITY, String.valueOf(user.getId()), null);
            }
            throw loginFailed(normalizedEmail, "wrong_password");
        }
        loginAttempts.recordSuccess(user.getId());
        AuthResult result = startSession(user);
        audit.record(AuditAction.LOGIN_SUCCESS, user.getEmail(), ENTITY, String.valueOf(user.getId()), null);
        return result;
    }

    public AuthResult refresh(String rawRefreshToken) {
        Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        switch (rotation.status()) {
            case REUSED -> {
                String actor = users.findById(rotation.adminId()).map(AdminUser::getEmail).orElse("unknown");
                audit.record(AuditAction.TOKEN_REUSE_DETECTED, actor, ENTITY, String.valueOf(rotation.adminId()), null);
                throw new UnauthorizedException(SESSION_EXPIRED);
            }
            case INVALID -> throw new UnauthorizedException(SESSION_EXPIRED);
            default -> {
                AdminUser user = users.findById(rotation.adminId())
                        .orElseThrow(() -> new UnauthorizedException(SESSION_EXPIRED));
                return new AuthResult(issueAccessToken(user), rotation.token());
            }
        }
    }

    public void logout(String rawRefreshToken) {
        refreshTokens.revokeFamily(rawRefreshToken)
                .flatMap(users::findById)
                .ifPresent(user -> audit.record(AuditAction.LOGOUT, user.getEmail(), ENTITY,
                        String.valueOf(user.getId()), null));
    }

    /** Changes the password, signs out every other session and returns a fresh session for this one. */
    @Transactional
    public AuthResult changePassword(long adminId, String currentPassword, String newPassword) {
        AdminUser user = users.findByIdForUpdate(adminId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        if (!passwordMatches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        PasswordPolicy.validate(newPassword);
        if (passwordMatches(newPassword, user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current one");
        }
        user.changePassword(passwordEncoder.encode(newPassword), Instant.now(clock));
        users.flush();
        refreshTokens.revokeAll(adminId);
        evictCredentialsCacheAfterCommit(adminId);
        audit.record(AuditAction.PASSWORD_CHANGED, user.getEmail(), ENTITY, String.valueOf(adminId),
                Map.of("sessionsRevoked", true));
        return startSession(user);
    }

    @Transactional(readOnly = true)
    public AdminProfile profile(long adminId) {
        return users.findById(adminId)
                .map(u -> new AdminProfile(u.getId(), u.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    private AuthResult startSession(AdminUser user) {
        IssuedRefreshToken refresh = refreshTokens.issueNewFamily(user.getId());
        return new AuthResult(issueAccessToken(user), refresh);
    }

    private IssuedAccessToken issueAccessToken(AdminUser user) {
        return accessTokens.issue(user.getId(), user.getEmail(), user.credentialsVersion());
    }

    private boolean passwordMatches(String raw, String hash) {
        if (raw == null || !PasswordPolicy.fitsBcrypt(raw)) {
            passwordEncoder.matches("x", dummyHash);
            return false;
        }
        return passwordEncoder.matches(raw, hash);
    }

    private UnauthorizedException loginFailed(String attemptedEmail, String reason) {
        audit.record(AuditAction.LOGIN_FAILURE, attemptedEmail, ENTITY, null, Map.of("reason", reason));
        return new UnauthorizedException(LOGIN_FAILED);
    }

    private void evictCredentialsCacheAfterCommit(long adminId) {
        credentialsCheck.evict(adminId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    credentialsCheck.evict(adminId);
                }
            });
        }
    }

    public record AuthResult(IssuedAccessToken accessToken, IssuedRefreshToken refreshToken) {
    }

    public record AdminProfile(long id, String email) {
    }
}
