package com.shady.landing.security;

import com.shady.landing.common.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Double-submit CSRF protection for the cookie-authenticated endpoints (refresh, logout). The token cookie
 * is readable by the frontend (shared parent domain in prod) and must be echoed in the CSRF header.
 */
@Configuration
public class CsrfConfig {

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(AppProperties properties) {
        AppProperties.Csrf csrf = properties.security().csrf();
        AppProperties.Refresh refresh = properties.security().refresh();
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(csrf.cookieName());
        repository.setHeaderName(csrf.headerName());
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> {
            cookie.secure(refresh.secure()).sameSite("Strict").maxAge(refresh.ttl());
            if (csrf.hasCookieDomain()) {
                cookie.domain(csrf.cookieDomain());
            }
        });
        return repository;
    }
}
