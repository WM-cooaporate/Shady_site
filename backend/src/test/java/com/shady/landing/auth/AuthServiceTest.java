package com.shady.landing.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shady.landing.audit.AuditAction;
import com.shady.landing.audit.AuditService;
import com.shady.landing.auth.RefreshTokenService.IssuedRefreshToken;
import com.shady.landing.auth.RefreshTokenService.Rotation;
import com.shady.landing.common.error.BadRequestException;
import com.shady.landing.common.error.UnauthorizedException;
import com.shady.landing.security.AccessTokenService;
import com.shady.landing.security.AccessTokenService.IssuedAccessToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");
    private static final String PASSWORD = "Correct-Horse-9!";

    @Mock AdminUserRepository users;
    @Mock LoginAttemptService loginAttempts;
    @Mock RefreshTokenService refreshTokens;
    @Mock AccessTokenService accessTokens;
    @Mock AdminCredentialsChangeCheck credentialsCheck;
    @Mock AuditService audit;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService service;
    private AdminUser admin;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, loginAttempts, refreshTokens, accessTokens, credentialsCheck, encoder, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new AdminUser("shady@shady.com", encoder.encode(PASSWORD), NOW.minusSeconds(3600));
        ReflectionTestUtils.setField(admin, "id", 1L);
        when(users.findByEmail("shady@shady.com")).thenReturn(Optional.of(admin));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(accessTokens.issue(anyLong(), anyString(), anyLong()))
                .thenReturn(new IssuedAccessToken("access", NOW.plusSeconds(900)));
        when(refreshTokens.issueNewFamily(1L))
                .thenReturn(new IssuedRefreshToken("refresh", NOW.plus(Duration.ofDays(7))));
    }

    @Test
    void loginWithCorrectPasswordStartsSession() {
        AuthService.AuthResult result = service.login(" Shady@Shady.com ", PASSWORD, "1.2.3.4");

        assertThat(result.accessToken().value()).isEqualTo("access");
        assertThat(result.refreshToken().value()).isEqualTo("refresh");
        verify(loginAttempts).recordSuccess(1L);
        verify(accessTokens).issue(1L, "shady@shady.com", admin.credentialsVersion());
        verify(audit).record(eq(AuditAction.LOGIN_SUCCESS), eq("shady@shady.com"), any(), any(), isNull());
    }

    @Test
    void wrongPasswordAndUnknownEmailFailWithTheSameGenericMessage() {
        assertThatThrownBy(() -> service.login("shady@shady.com", "wrong-password", "1.2.3.4"))
                .isInstanceOf(UnauthorizedException.class).hasMessage(AuthService.LOGIN_FAILED);
        assertThatThrownBy(() -> service.login("nobody@shady.com", PASSWORD, "1.2.3.4"))
                .isInstanceOf(UnauthorizedException.class).hasMessage(AuthService.LOGIN_FAILED);

        verify(loginAttempts).recordFailure(1L, "1.2.3.4");
        verify(refreshTokens, never()).issueNewFamily(anyLong());
    }

    @Test
    void failureThatLocksTheAccountIsAudited() {
        when(loginAttempts.recordFailure(1L, "1.2.3.4")).thenReturn(true);

        assertThatThrownBy(() -> service.login("shady@shady.com", "wrong-password", "1.2.3.4"))
                .isInstanceOf(UnauthorizedException.class);

        verify(audit).record(eq(AuditAction.ACCOUNT_LOCKED), eq("shady@shady.com"), any(), eq("1"), isNull());
    }

    @Test
    void lockedAccountRejectsEvenTheCorrectPassword() {
        admin.registerFailedLogin(1, Duration.ofMinutes(15), NOW);

        assertThatThrownBy(() -> service.login("shady@shady.com", PASSWORD, "1.2.3.4"))
                .isInstanceOf(UnauthorizedException.class).hasMessage(AuthService.LOGIN_FAILED);
        verify(loginAttempts, never()).recordSuccess(anyLong());
        verify(loginAttempts, never()).recordFailure(anyLong(), any());
    }

    @Test
    void passwordLongerThanBcryptLimitNeverMatches() {
        String tooLong = PASSWORD + "x".repeat(80);
        assertThatThrownBy(() -> service.login("shady@shady.com", tooLong, "1.2.3.4"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshWithReusedTokenIsRejectedAndAudited() {
        when(refreshTokens.rotate("stolen")).thenReturn(new Rotation(Rotation.Status.REUSED, 1L, null));

        assertThatThrownBy(() -> service.refresh("stolen")).isInstanceOf(UnauthorizedException.class);
        verify(audit).record(eq(AuditAction.TOKEN_REUSE_DETECTED), eq("shady@shady.com"), any(), eq("1"), isNull());
        verify(accessTokens, never()).issue(anyLong(), anyString(), anyLong());
    }

    @Test
    void refreshWithValidTokenIssuesNewAccessToken() {
        IssuedRefreshToken next = new IssuedRefreshToken("next", NOW.plus(Duration.ofDays(7)));
        when(refreshTokens.rotate("good")).thenReturn(new Rotation(Rotation.Status.ROTATED, 1L, next));

        AuthService.AuthResult result = service.refresh("good");

        assertThat(result.refreshToken()).isEqualTo(next);
        assertThat(result.accessToken().value()).isEqualTo("access");
    }

    @Test
    void changePasswordRevokesSessionsAndBumpsCredentialsVersion() {
        long versionBefore = admin.credentialsVersion();

        service.changePassword(1L, PASSWORD, "A-Brand-New-Passw0rd");

        assertThat(encoder.matches("A-Brand-New-Passw0rd", admin.getPasswordHash())).isTrue();
        assertThat(admin.credentialsVersion()).isNotEqualTo(versionBefore);
        verify(refreshTokens).revokeAll(1L);
        verify(credentialsCheck).evict(1L);
        verify(audit).record(eq(AuditAction.PASSWORD_CHANGED), eq("shady@shady.com"), any(), eq("1"), any());
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndPolicy() {
        assertThatThrownBy(() -> service.changePassword(1L, "wrong", "A-Brand-New-Passw0rd"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.changePassword(1L, PASSWORD, "short"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.changePassword(1L, PASSWORD, PASSWORD))
                .isInstanceOf(BadRequestException.class);
        verify(refreshTokens, never()).revokeAll(anyLong());
    }
}
