package com.shady.landing.common.web;

import com.shady.landing.common.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP used for rate limiting and audit. A proxy header (e.g. {@code CF-Connecting-IP})
 * is trusted only when configured, which must only be done when the origin is reachable solely via that proxy.
 */
@Component
public class ClientIpResolver {

    private static final Pattern IP_CHARS = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");

    private final AppProperties.Security security;

    public ClientIpResolver(AppProperties properties) {
        this.security = properties.security();
    }

    public String resolve(HttpServletRequest request) {
        if (security.hasClientIpHeader()) {
            String header = request.getHeader(security.clientIpHeader());
            if (header != null) {
                String candidate = header.split(",")[0].trim();
                if (IP_CHARS.matcher(candidate).matches()) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
