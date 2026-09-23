package com.shady.landing.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * All application settings, bound from {@code app.*} (values come from environment variables).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Security security,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Cors cors,
        @Valid @NotNull List<RateLimitRule> rateLimits) {

    public record Security(
            @Valid @NotNull Jwt jwt,
            @Valid @NotNull Refresh refresh,
            @Valid @NotNull Csrf csrf,
            @Valid @NotNull Lockout lockout,
            @Min(4) @Max(16) int bcryptStrength,
            /* Header carrying the real client IP (e.g. CF-Connecting-IP). Only set when the app is reachable solely through that proxy. */
            String clientIpHeader) {

        public boolean hasClientIpHeader() {
            return StringUtils.hasText(clientIpHeader);
        }
    }

    /** {@code secret} is a base64-encoded HMAC key of at least 256 bits. */
    public record Jwt(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration accessTokenTtl) {
    }

    public record Refresh(@NotNull Duration ttl, @NotBlank String cookieName, @NotBlank String cookiePath, boolean secure) {
    }

    /** {@code cookieDomain} is blank in dev (host-only cookie) and e.g. {@code shady.com} in prod. */
    public record Csrf(@NotBlank String cookieName, @NotBlank String headerName, String cookieDomain) {

        public boolean hasCookieDomain() {
            return StringUtils.hasText(cookieDomain);
        }
    }

    public record Lockout(@Min(1) int maxAttempts, @NotNull Duration duration) {
    }

    /** Bootstrap admin, created on first startup only. */
    public record Admin(String email, String initialPassword) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record RateLimitRule(
            @NotBlank String name,
            @NotBlank String method,
            @NotBlank String path,
            @Min(1) long capacity,
            @NotNull Duration period) {
    }
}
