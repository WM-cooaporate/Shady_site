package com.shady.landing.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.shady.landing.support.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Checks the exact Set-Cookie headers produced by the real servlet container (not MockMvc). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CookieAttributesOverHttpIT extends IntegrationTest {

    @LocalServerPort
    int port;

    @Test
    void loginCookiesHaveStrictAttributesOnTheWire() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Client-IP", uniqueIp())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> cookies = response.headers().allValues("Set-Cookie");
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("shady_rt=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth"));
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("XSRF-TOKEN=")
                .contains("Secure", "SameSite=Strict", "Path=/")
                .doesNotContainIgnoringCase("HttpOnly"));
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
    }
}
