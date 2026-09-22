package com.shady.landing.common.ratelimit;

import com.shady.landing.common.config.AppProperties;
import com.shady.landing.common.config.AppProperties.RateLimitRule;
import com.shady.landing.common.error.ApiErrorWriter;
import com.shady.landing.common.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the configured per-IP rules (login, uploads, clicks, ...). Registered inside the security chain
 * right after CORS, so 429 responses still carry CORS headers. Not a bean, to avoid double registration.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final AntPathMatcher PATHS = new AntPathMatcher();

    private final List<RateLimitRule> rules;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final ApiErrorWriter errorWriter;

    public RateLimitFilter(AppProperties properties, RateLimiter rateLimiter, ClientIpResolver clientIpResolver,
                           ApiErrorWriter errorWriter) {
        this.rules = List.copyOf(properties.rateLimits());
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        for (RateLimitRule rule : rules) {
            if (rule.method().equalsIgnoreCase(request.getMethod()) && PATHS.match(rule.path(), path)) {
                String ip = clientIpResolver.resolve(request);
                RateLimiter.Result result = rateLimiter.tryConsume(rule, ip);
                if (!result.allowed()) {
                    log.warn("Rate limit '{}' exceeded by {}", rule.name(), ip);
                    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
                    errorWriter.write(response, request, HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
