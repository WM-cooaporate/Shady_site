package com.shady.landing.auth.dto;

import java.time.Instant;

/** The refresh token is never in the body; it is set as an HttpOnly cookie. */
public record AccessTokenResponse(String accessToken, String tokenType, Instant expiresAt, long expiresIn) {
}
