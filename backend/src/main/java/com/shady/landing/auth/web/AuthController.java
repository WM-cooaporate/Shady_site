package com.shady.landing.auth.web;

import com.shady.landing.auth.AuthService;
import com.shady.landing.auth.AuthService.AuthResult;
import com.shady.landing.auth.dto.AccessTokenResponse;
import com.shady.landing.auth.dto.AdminProfileResponse;
import com.shady.landing.auth.dto.ChangePasswordRequest;
import com.shady.landing.auth.dto.LoginRequest;
import com.shady.landing.common.config.OpenApiConfig;
import com.shady.landing.common.error.UnauthorizedException;
import com.shady.landing.common.web.ClientIpResolver;
import com.shady.landing.security.CurrentAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies cookies;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final ClientIpResolver clientIpResolver;
    private final Clock clock;

    public AuthController(AuthService authService, AuthCookies cookies, CookieCsrfTokenRepository csrfTokenRepository,
                          ClientIpResolver clientIpResolver, Clock clock) {
        this.authService = authService;
        this.cookies = cookies;
        this.csrfTokenRepository = csrfTokenRepository;
        this.clientIpResolver = clientIpResolver;
        this.clock = clock;
    }

    @Operation(summary = "Sign in. Returns an access token and sets the refresh + CSRF cookies.")
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                                                     HttpServletResponse response) {
        AuthResult result = authService.login(body.email(), body.password(), clientIpResolver.resolve(request));
        return session(result, request, response);
    }

    @Operation(summary = "Rotate the refresh cookie and get a new access token. Requires the X-XSRF-TOKEN header.")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookies.readRefreshToken(request);
        if (refreshToken == null) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        try {
            return session(authService.refresh(refreshToken), request, response);
        } catch (UnauthorizedException e) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookies.clearedRefreshCookie().toString());
            throw e;
        }
    }

    @Operation(summary = "Sign out: revokes the refresh token family and clears cookies. Requires X-XSRF-TOKEN.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(cookies.readRefreshToken(request));
        csrfTokenRepository.saveToken(null, request, response);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearedRefreshCookie().toString())
                .build();
    }

    @Operation(summary = "Change the admin password. Signs out all other sessions.")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @PostMapping("/change-password")
    public ResponseEntity<AccessTokenResponse> changePassword(@Valid @RequestBody ChangePasswordRequest body,
                                                              HttpServletRequest request, HttpServletResponse response) {
        AuthResult result = authService.changePassword(CurrentAdmin.requireId(), body.currentPassword(),
                body.newPassword());
        return session(result, request, response);
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @GetMapping("/me")
    public AdminProfileResponse me() {
        AuthService.AdminProfile profile = authService.profile(CurrentAdmin.requireId());
        return new AdminProfileResponse(profile.id(), profile.email());
    }

    private ResponseEntity<AccessTokenResponse> session(AuthResult result, HttpServletRequest request,
                                                        HttpServletResponse response) {
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        Instant expiresAt = result.accessToken().expiresAt();
        long expiresIn = Math.max(0, Duration.between(Instant.now(clock), expiresAt).toSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.refreshCookie(result.refreshToken()).toString())
                .body(new AccessTokenResponse(result.accessToken().value(), "Bearer", expiresAt, expiresIn));
    }
}
