package com.shady.landing.auth;

import com.shady.landing.common.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque, rotating refresh tokens. The raw value (256 random bits) only ever lives in the HttpOnly cookie;
 * the database stores its SHA-256. Presenting an already-rotated token revokes the whole family.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final AppProperties.Refresh settings;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.settings = properties.security().refresh();
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issueNewFamily(long adminId) {
        return issue(adminId, UUID.randomUUID(), now()).issued();
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Rotation.invalid();
        }
        Optional<RefreshToken> found = repository.findByTokenHashForUpdate(hash(rawToken));
        if (found.isEmpty()) {
            return Rotation.invalid();
        }
        RefreshToken current = found.get();
        Instant now = now();
        if (current.isRevoked()) {
            if (current.getReplacedById() != null) {
                // A rotated token was used again: it was stolen or replayed. Kill the whole session family.
                repository.revokeFamily(current.getFamilyId(), now);
                return new Rotation(Rotation.Status.REUSED, current.getAdminUserId(), null);
            }
            return Rotation.invalid();
        }
        if (current.isExpired(now)) {
            return Rotation.invalid();
        }
        Issued next = issue(current.getAdminUserId(), current.getFamilyId(), now);
        current.markRotated(next.entity().getId(), now);
        return new Rotation(Rotation.Status.ROTATED, current.getAdminUserId(), next.issued());
    }

    /** Revokes the family of the given token; returns the owning admin id if the token was known. */
    @Transactional
    public Optional<Long> revokeFamily(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHashForUpdate(hash(rawToken)).map(token -> {
            repository.revokeFamily(token.getFamilyId(), now());
            return token.getAdminUserId();
        });
    }

    @Transactional
    public void revokeAll(long adminId) {
        repository.revokeAllForAdmin(adminId, now());
    }

    @Transactional
    public int deleteExpired(Instant cutoff) {
        return repository.deleteExpiredBefore(cutoff);
    }

    private Issued issue(long adminId, UUID familyId, Instant now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(settings.ttl());
        RefreshToken entity = repository.save(new RefreshToken(adminId, familyId, hash(raw), now, expiresAt));
        return new Issued(entity, new IssuedRefreshToken(raw, expiresAt));
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private record Issued(RefreshToken entity, IssuedRefreshToken issued) {
    }

    public record IssuedRefreshToken(String value, Instant expiresAt) {
    }

    public record Rotation(Status status, Long adminId, IssuedRefreshToken token) {

        public enum Status { ROTATED, REUSED, INVALID }

        static Rotation invalid() {
            return new Rotation(Status.INVALID, null, null);
        }
    }
}
