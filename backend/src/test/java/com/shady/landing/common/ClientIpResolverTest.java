package com.shady.landing.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.shady.landing.common.config.AppProperties;
import com.shady.landing.common.web.ClientIpResolver;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void ignoresProxyHeaderWhenNotConfigured() {
        MockHttpServletRequest request = request("CF-Connecting-IP", "9.9.9.9");
        assertThat(resolver(null).resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void usesConfiguredHeader() {
        MockHttpServletRequest request = request("CF-Connecting-IP", "203.0.113.7");
        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void rejectsGarbageHeaderValues() {
        MockHttpServletRequest request = request("CF-Connecting-IP", "<script>");
        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo("10.0.0.1");
    }

    private static MockHttpServletRequest request(String header, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader(header, value);
        return request;
    }

    private static ClientIpResolver resolver(String header) {
        AppProperties.Security security = new AppProperties.Security(
                new AppProperties.Jwt("secret", "issuer", Duration.ofMinutes(15)),
                new AppProperties.Refresh(Duration.ofDays(7), "rt", "/", true),
                new AppProperties.Csrf("XSRF-TOKEN", "X-XSRF-TOKEN", null),
                new AppProperties.Lockout(5, Duration.ofMinutes(15)),
                10, header);
        return new ClientIpResolver(new AppProperties(security, new AppProperties.Admin(null, null),
                new AppProperties.Cors(List.of("https://shady.com")), List.of()));
    }
}
