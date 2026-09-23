package com.shady.landing.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.shady.landing.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Discovers every admin endpoint from the MVC mappings (so new endpoints are covered automatically) and checks
 * that none of them can be reached without a valid ADMIN access token.
 */
class AdminEndpointsSecurityIT extends IntegrationTest {

    private static final Set<String> ANONYMOUS_AUTH_ENDPOINTS =
            Set.of(AuthPaths.LOGIN, AuthPaths.REFRESH, AuthPaths.LOGOUT);

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @Value("${app.security.jwt.secret}")
    String jwtSecret;

    @Value("${app.security.jwt.issuer}")
    String issuer;

    record Endpoint(HttpMethod method, String path) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    List<Endpoint> protectedEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPatternValues()) {
                boolean admin = pattern.startsWith("/api/v1/admin/") || pattern.equals("/api/v1/admin");
                boolean auth = pattern.startsWith(AuthPaths.BASE + "/") && !ANONYMOUS_AUTH_ENDPOINTS.contains(pattern);
                if (!admin && !auth) {
                    continue;
                }
                String path = pattern.replaceAll("\\{[^}]+}", "1");
                if (methods.isEmpty()) {
                    endpoints.add(new Endpoint(HttpMethod.GET, path));
                } else {
                    methods.forEach(m -> endpoints.add(new Endpoint(HttpMethod.valueOf(m.name()), path)));
                }
            }
        }
        return endpoints;
    }

    @Test
    void discoversTheAdminEndpoints() {
        assertThat(protectedEndpoints()).extracting(Endpoint::toString)
                .contains("GET /api/v1/admin/audit-log", "GET /api/v1/auth/me", "POST /api/v1/auth/change-password");
    }

    @TestFactory
    Stream<DynamicTest> everyAdminEndpointRejectsMissingOrInvalidTokens() {
        long adminId = adminId();
        long version = jdbc.queryForObject("select password_changed_at from admin_user where id = ?",
                java.sql.Timestamp.class, adminId).toInstant().toEpochMilli();
        SecretKey realKey = new SecretKeySpec(Base64.getDecoder().decode(jwtSecret), "HmacSHA256");
        SecretKey otherKey = new SecretKeySpec(Base64.getDecoder().decode(
                "b3RoZXIta2V5LW90aGVyLWtleS1vdGhlci1rZXktMzI="), "HmacSHA256");
        Instant now = Instant.now();

        List<Scenario> scenarios = List.of(
                new Scenario("no token", () -> null, 401),
                new Scenario("malformed token", () -> "not-a-jwt", 401),
                new Scenario("wrong signing key",
                        () -> token(otherKey, issuer, adminId, List.of("ADMIN"), version, now, now.plusSeconds(600)), 401),
                new Scenario("expired token",
                        () -> token(realKey, issuer, adminId, List.of("ADMIN"), version,
                                now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1))), 401),
                new Scenario("wrong issuer",
                        () -> token(realKey, "https://evil.example", adminId, List.of("ADMIN"), version, now,
                                now.plusSeconds(600)), 401),
                new Scenario("revoked credentials version",
                        () -> token(realKey, issuer, adminId, List.of("ADMIN"), version - 1, now, now.plusSeconds(600)),
                        401),
                new Scenario("valid token without ADMIN role",
                        () -> token(realKey, issuer, adminId, List.of("USER"), version, now, now.plusSeconds(600)), 403));

        return protectedEndpoints().stream().flatMap(endpoint -> scenarios.stream().map(scenario ->
                DynamicTest.dynamicTest(endpoint + " with " + scenario.name + " -> " + scenario.expectedStatus, () -> {
                    MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                            .request(endpoint.method(), endpoint.path())
                            .secure(true)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}");
                    String token = scenario.token.get();
                    if (token != null) {
                        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                    mvc.perform(request).andExpect(status().is(scenario.expectedStatus));
                })));
    }

    private record Scenario(String name, Supplier<String> token, int expectedStatus) {
    }

    private static String token(SecretKey key, String iss, long subject, List<String> roles, long version,
                                Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(iss)
                .subject(String.valueOf(subject))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(AccessTokenService.CLAIM_EMAIL, ADMIN_EMAIL)
                .claim(AccessTokenService.CLAIM_ROLES, roles)
                .claim(AccessTokenService.CLAIM_CREDENTIALS_VERSION, version)
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
