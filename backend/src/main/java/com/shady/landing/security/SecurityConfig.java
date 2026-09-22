package com.shady.landing.security;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

import com.shady.landing.common.config.AppProperties;
import com.shady.landing.common.error.ApiErrorWriter;
import com.shady.landing.common.ratelimit.RateLimitFilter;
import com.shady.landing.common.ratelimit.RateLimiter;
import com.shady.landing.common.web.ClientIpResolver;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Deny-by-default security. Anonymous access: public GETs, the click endpoint, login/refresh/logout and
 * /actuator/health. Everything under /api/v1/admin/** and the remaining /api/v1/auth/** requires ROLE_ADMIN.
 * Anything else is denied.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_CSP =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    private static final String DOCS_CSP = "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
            + "script-src 'self' 'unsafe-inline'; frame-ancestors 'none'";
    private static final long HSTS_MAX_AGE = 31_536_000; // 1 year

    /** Swagger UI + OpenAPI JSON (dev/test only; springdoc is disabled in prod as well). */
    @Bean
    @Order(1)
    @Profile("!prod")
    SecurityFilterChain apiDocsSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .anyRequest().denyAll())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(DOCS_CSP))
                        .frameOptions(f -> f.deny())
                        .cacheControl(c -> c.disable()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               CookieCsrfTokenRepository csrfTokenRepository,
                                               RestAuthenticationEntryPoint entryPoint,
                                               RestAccessDeniedHandler accessDeniedHandler,
                                               RateLimitFilter rateLimitFilter) throws Exception {
        RequestMatcher cookieAuthenticated = new OrRequestMatcher(
                withDefaults().matcher(HttpMethod.POST, AuthPaths.REFRESH),
                withDefaults().matcher(HttpMethod.POST, AuthPaths.LOGOUT));

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(cookieAuthenticated))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.GET, AuthPaths.PUBLIC).permitAll()
                        .requestMatchers(HttpMethod.POST, AuthPaths.PUBLIC_CLICKS).permitAll()
                        .requestMatchers(HttpMethod.POST, AuthPaths.LOGIN, AuthPaths.REFRESH, AuthPaths.LOGOUT).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(AuthPaths.ADMIN, AuthPaths.BASE + "/**").hasRole(AccessTokenService.ROLE_ADMIN)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(h -> h
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(API_CSP))
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        .cacheControl(c -> c.disable()) // handled by CacheControlFilter
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()")))
                .addFilterAfter(rateLimitFilter, CorsFilter.class);
        return http.build();
    }

    @Bean
    RateLimitFilter rateLimitFilter(AppProperties properties, RateLimiter rateLimiter,
                                    ClientIpResolver clientIpResolver, ApiErrorWriter errorWriter) {
        return new RateLimitFilter(properties, rateLimiter, clientIpResolver, errorWriter);
    }

    /** Declared so Spring Boot does not also register the filter on the servlet container. */
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder(AppProperties properties) {
        return new BCryptPasswordEncoder(properties.security().bcryptStrength());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", properties.security().csrf().headerName()));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * Ignores the Authorization header on anonymous endpoints, so a stale token sent by the frontend never
     * turns a public request into a 401.
     */
    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        RequestMatcher anonymous = new OrRequestMatcher(
                withDefaults().matcher(AuthPaths.PUBLIC),
                withDefaults().matcher(AuthPaths.LOGIN),
                withDefaults().matcher(AuthPaths.REFRESH),
                withDefaults().matcher(AuthPaths.LOGOUT));
        return request -> anonymous.matches(request) ? null : delegate.resolve(request);
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(AccessTokenService.CLAIM_ROLES);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
