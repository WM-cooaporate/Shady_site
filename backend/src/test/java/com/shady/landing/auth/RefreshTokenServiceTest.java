package com.shady.landing.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shady.landing.auth.RefreshTokenService.Rotation;
import com.shady.landing.common.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");

    @Mock RefreshTokenRepository repository;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        AppProperties.Security security = new AppProperties.Security(
                new AppProperties.Jwt("s", "i", Duration.ofMinutes(15)),
                new AppProperties.Refresh(Duration.ofDays(7), "rt", "/api/v1/auth", true),
                new AppProperties.Csrf("XSRF-TOKEN", "X-XSRF-TOKEN", null),
                new AppProperties.Lockout(5, Duration.ofMinutes(15)), 4, null);
        AppProperties properties = new AppProperties(security, new AppProperties.Admin(null, null),
                new AppProperties.Cors(List.of("https://shady.com")), List.of());
        service = new RefreshTokenService(repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issuedTokenIsStoredOnlyAsHash() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued = service.issueNewFamily(1L);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(issued.value()).hasSizeGreaterThanOrEqualTo(43); // 256 bits, base64url
        assertThat(saved.getValue()).extracting("tokenHash").isEqualTo(RefreshTokenService.hash(issued.value()));
        assertThat(saved.getValue()).extracting("tokenHash").isNotEqualTo(issued.value());
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void rotationRevokesOldTokenAndKeepsFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken current = new RefreshToken(1L, family, RefreshTokenService.hash("old"), NOW.minusSeconds(60),
                NOW.plus(Duration.ofDays(1)));
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("old"))).thenReturn(Optional.of(current));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Rotation rotation = service.rotate("old");

        assertThat(rotation.status()).isEqualTo(Rotation.Status.ROTATED);
        assertThat(rotation.token().value()).isNotEqualTo("old");
        assertThat(current.isRevoked()).isTrue();
        assertThat(current.getReplacedById()).isNotNull();
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(family);
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken rotated = new RefreshToken(1L, family, RefreshTokenService.hash("old"), NOW.minusSeconds(60),
                NOW.plus(Duration.ofDays(1)));
        rotated.markRotated(UUID.randomUUID(), NOW.minusSeconds(30));
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("old"))).thenReturn(Optional.of(rotated));

        Rotation rotation = service.rotate("old");

        assertThat(rotation.status()).isEqualTo(Rotation.Status.REUSED);
        verify(repository).revokeFamily(family, NOW);
        verify(repository, never()).save(any());
    }

    @Test
    void expiredOrUnknownTokensAreInvalid() {
        RefreshToken expired = new RefreshToken(1L, UUID.randomUUID(), RefreshTokenService.hash("exp"),
                NOW.minus(Duration.ofDays(8)), NOW.minusSeconds(1));
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("exp"))).thenReturn(Optional.of(expired));
        when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("nope"))).thenReturn(Optional.empty());

        assertThat(service.rotate("exp").status()).isEqualTo(Rotation.Status.INVALID);
        assertThat(service.rotate("nope").status()).isEqualTo(Rotation.Status.INVALID);
        assertThat(service.rotate(null).status()).isEqualTo(Rotation.Status.INVALID);
        verify(repository, never()).save(any());
    }
}
