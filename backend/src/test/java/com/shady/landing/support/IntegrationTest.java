package com.shady.landing.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shady.landing.common.cache.CacheNames;
import com.shady.landing.common.cache.CacheService;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests: full Spring context + real PostgreSQL 16.
 *
 * <p>By default a Testcontainers PostgreSQL is started once and shared by all test classes. Where Docker is not
 * available, set {@code SHADY_TEST_DB_URL} (plus {@code SHADY_TEST_DB_USERNAME} / {@code SHADY_TEST_DB_PASSWORD})
 * to run against an existing, disposable database instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    public static final String ADMIN_EMAIL = "admin@shady.test";
    public static final String ADMIN_PASSWORD = "Initial-Passw0rd!";

    private static final String EXTERNAL_DB_URL = System.getenv("SHADY_TEST_DB_URL");
    private static final PostgreSQLContainer<?> POSTGRES = EXTERNAL_DB_URL == null ? startPostgres() : null;
    private static final AtomicInteger IP_COUNTER = new AtomicInteger();

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CacheService cacheService;

    @SuppressWarnings("resource")
    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB_URL);
            registry.add("spring.datasource.username", () -> envOr("SHADY_TEST_DB_USERNAME", "postgres"));
            registry.add("spring.datasource.password", () -> envOr("SHADY_TEST_DB_PASSWORD", ""));
        }
    }

    /** Every test starts with the bootstrap admin in a clean, unlocked state and no sessions. */
    @BeforeEach
    void resetAuthState() {
        jdbc.update("delete from refresh_token");
        jdbc.update("delete from audit_log");
        jdbc.update("""
                update admin_user set password_hash = ?, failed_login_attempts = 0, locked_until = null,
                       password_changed_at = date_trunc('milliseconds', now() - interval '1 hour')""",
                passwordEncoder.encode(ADMIN_PASSWORD));
        cacheService.evictAll(CacheNames.ADMIN_CREDENTIALS);
    }

    /** A fresh client IP per call, so rate-limit buckets never leak between tests. */
    protected static String uniqueIp() {
        int n = IP_COUNTER.incrementAndGet();
        return "10.%d.%d.%d".formatted((n >> 16) & 255, (n >> 8) & 255, n & 255);
    }

    protected long adminId() {
        return jdbc.queryForObject("select id from admin_user where lower(email) = ?", Long.class, ADMIN_EMAIL);
    }

    protected Session login() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected Session login(String email, String password) throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/login")
                        .secure(true)
                        .header("X-Client-IP", uniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        return Session.from(response, objectMapper);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    public record Session(String accessToken, Cookie refreshCookie, Cookie csrfCookie) {

        public static Session from(MockHttpServletResponse response, ObjectMapper mapper) throws Exception {
            JsonNode body = mapper.readTree(response.getContentAsString());
            return new Session(body.get("accessToken").asText(), response.getCookie("shady_rt"),
                    response.getCookie("XSRF-TOKEN"));
        }

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
