package com.shady.landing.security;

import com.shady.landing.common.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived HS256 access tokens for the admin. */
@Service
public class AccessTokenService {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    /** Credentials version: the admin's password-change timestamp (epoch millis) when the token was issued. */
    public static final String CLAIM_CREDENTIALS_VERSION = "cv";
    public static final String ROLE_ADMIN = "ADMIN";

    private final JwtEncoder encoder;
    private final AppProperties.Jwt jwt;
    private final Clock clock;

    public AccessTokenService(JwtEncoder encoder, AppProperties properties, Clock clock) {
        this.encoder = encoder;
        this.jwt = properties.security().jwt();
        this.clock = clock;
    }

    public IssuedAccessToken issue(long adminId, String email, long credentialsVersion) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(jwt.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwt.issuer())
                .subject(String.valueOf(adminId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, List.of(ROLE_ADMIN))
                .claim(CLAIM_CREDENTIALS_VERSION, credentialsVersion)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {
    }
}
