package com.shady.landing.auth.web;

import com.shady.landing.auth.RefreshTokenService.IssuedRefreshToken;
import com.shady.landing.common.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/** Refresh cookie: HttpOnly, Secure, SameSite=Strict, host-only, scoped to /api/v1/auth. */
@Component
public class AuthCookies {

    private final AppProperties.Refresh settings;
    private final Clock clock;

    public AuthCookies(AppProperties properties, Clock clock) {
        this.settings = properties.security().refresh();
        this.clock = clock;
    }

    public ResponseCookie refreshCookie(IssuedRefreshToken token) {
        Duration maxAge = Duration.between(Instant.now(clock), token.expiresAt());
        return base(token.value()).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
    }

    public ResponseCookie clearedRefreshCookie() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String readRefreshToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, settings.cookieName());
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(settings.cookieName(), value)
                .httpOnly(true)
                .secure(settings.secure())
                .sameSite("Strict")
                .path(settings.cookiePath());
    }
}
