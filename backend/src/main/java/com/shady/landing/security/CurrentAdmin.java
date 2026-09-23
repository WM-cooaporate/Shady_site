package com.shady.landing.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Access to the authenticated admin of the current request (from the validated access token). */
public final class CurrentAdmin {

    private CurrentAdmin() {
    }

    public static Optional<Jwt> token() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    public static Optional<Long> id() {
        return token().map(jwt -> Long.valueOf(jwt.getSubject()));
    }

    public static Optional<String> email() {
        return token().map(jwt -> jwt.getClaimAsString(AccessTokenService.CLAIM_EMAIL));
    }

    public static long requireId() {
        return id().orElseThrow(() -> new IllegalStateException("No authenticated admin"));
    }
}
