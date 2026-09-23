package com.shady.landing.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shady.landing.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

class AuthFlowIT extends IntegrationTest {

    @Test
    void loginReturnsAccessTokenAndHardenedCookies() throws Exception {
        MockHttpServletResponse response = loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD, uniqueIp())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(allOf(greaterThan(890), lessThanOrEqualTo(900))))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn().getResponse();

        List<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        String refresh = setCookies.stream().filter(c -> c.startsWith("shady_rt=")).findFirst().orElseThrow();
        assertThat(refresh).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
        String csrf = setCookies.stream().filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        // SameSite on this cookie is asserted over real HTTP in CookieAttributesOverHttpIT (MockMvc drops it).
        assertThat(csrf).contains("Secure", "Path=/").doesNotContain("HttpOnly");

        String hash = jdbc.queryForObject("select token_hash from refresh_token", String.class);
        assertThat(refresh).doesNotContain(hash);
    }

    @Test
    void wrongPasswordAndUnknownEmailLookIdentical() throws Exception {
        String wrongPassword = loginRequest(ADMIN_EMAIL, "Wrong-Password-1", uniqueIp())
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = loginRequest("nobody@shady.test", "Wrong-Password-1", uniqueIp())
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(wrongPassword).get("message"))
                .isEqualTo(objectMapper.readTree(unknownEmail).get("message"));
        assertThat(wrongPassword).contains("Invalid email or password");
        assertThat(jdbc.queryForObject("select count(*) from audit_log where action = 'LOGIN_FAILURE'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void invalidLoginBodyIsRejectedWithFieldErrors() throws Exception {
        loginRequest("not-an-email", "", uniqueIp())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

    @Test
    void unknownJsonFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").secure(true).header("X-Client-IP", uniqueIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD, "role", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accessTokenGrantsAdminAccess() throws Exception {
        Session session = login();
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    @Test
    void refreshRequiresMatchingCsrfHeader() throws Exception {
        Session session = login();
        mvc.perform(post("/api/v1/auth/refresh").secure(true)
                        .cookie(session.refreshCookie(), session.csrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Missing or invalid CSRF token"));
        mvc.perform(post("/api/v1/auth/refresh").secure(true)
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesTokenAndReuseRevokesTheFamily() throws Exception {
        Session session = login();

        MockHttpServletResponse refreshed = refresh(session.refreshCookie(), session.csrfCookie())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse();
        Cookie rotated = refreshed.getCookie("shady_rt");
        assertThat(rotated.getValue()).isNotEqualTo(session.refreshCookie().getValue());

        // Replaying the old token: rejected, and the rotated one dies with it.
        refresh(session.refreshCookie(), session.csrfCookie()).andExpect(status().isUnauthorized());
        refresh(rotated, session.csrfCookie()).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject(
                "select count(*) from audit_log where action = 'TOKEN_REUSE_DETECTED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void logoutRevokesSessionAndClearsCookies() throws Exception {
        Session session = login();

        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/logout").secure(true)
                        .cookie(session.refreshCookie(), session.csrfCookie())
                        .header("X-XSRF-TOKEN", session.csrfCookie().getValue()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(c -> assertThat(c).startsWith("shady_rt=;").contains("Max-Age=0"));

        refresh(session.refreshCookie(), session.csrfCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        Session session = login();
        mvc.perform(post("/api/v1/auth/refresh").secure(true)
                        .cookie(session.csrfCookie())
                        .header("X-XSRF-TOKEN", session.csrfCookie().getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountLocksAfterRepeatedFailures() throws Exception {
        String ip = uniqueIp();
        for (int i = 0; i < 5; i++) {
            loginRequest(ADMIN_EMAIL, "Wrong-Password-" + i, ip).andExpect(status().isUnauthorized());
        }
        // Even the right password is refused while locked, with the same generic message.
        loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD, uniqueIp())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        assertThat(jdbc.queryForObject("select locked_until > now() from admin_user", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from audit_log where action = 'ACCOUNT_LOCKED'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void loginIsRateLimitedPerIp() throws Exception {
        String ip = uniqueIp();
        for (int i = 0; i < 10; i++) {
            loginRequest("nobody@shady.test", "Wrong-Password-1", ip).andExpect(status().isUnauthorized());
        }
        loginRequest("nobody@shady.test", "Wrong-Password-1", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        // Another client is not affected.
        loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD, uniqueIp()).andExpect(status().isOk());
    }

    @Test
    void changePasswordInvalidatesEveryExistingToken() throws Exception {
        Session other = login();
        Session current = login();

        MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/change-password")
                        .secure(true)
                        .header(HttpHeaders.AUTHORIZATION, current.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", ADMIN_PASSWORD, "newPassword", "Brand-New-Passw0rd!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Session renewed = Session.from(response, objectMapper);

        for (Session old : List.of(other, current)) {
            mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, old.bearer()))
                    .andExpect(status().isUnauthorized());
            refresh(old.refreshCookie(), old.csrfCookie()).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, renewed.bearer()))
                .andExpect(status().isOk());
        loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD, uniqueIp()).andExpect(status().isUnauthorized());
        loginRequest(ADMIN_EMAIL, "Brand-New-Passw0rd!", uniqueIp()).andExpect(status().isOk());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        Session session = login();
        mvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "Not-The-Passw0rd", "newPassword", "Brand-New-Passw0rd!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Current password")));
    }

    @Test
    void auditLogRecordsLoginsAndIsReadableByAdmin() throws Exception {
        Session session = login();
        mvc.perform(get("/api/v1/admin/audit-log").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items[0].action").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.items[0].actor").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private ResultActions loginRequest(String email, String password, String ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .secure(true)
                .header("X-Client-IP", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private ResultActions refresh(Cookie refreshCookie, Cookie csrfCookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh")
                .secure(true)
                .header("X-Client-IP", uniqueIp())
                .cookie(refreshCookie, csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()));
    }
}
