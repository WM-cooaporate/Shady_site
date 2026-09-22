package com.shady.landing.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Public GETs are cacheable by browsers (1 min) and the CDN (5 min); everything else, including admin and
 * auth responses, is {@code no-store}. Error responses override this with {@code no-store}.
 *
 * <p>Controllers must not set Cache-Control through {@code ResponseEntity}: Spring would add a second header
 * rather than replace this one. To change it, call {@code response.setHeader} instead.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CacheControlFilter extends OncePerRequestFilter {

    public static final String PUBLIC_PREFIX = "/api/v1/public/";
    public static final String PUBLIC_CACHE = "public, max-age=60, s-maxage=300, stale-while-revalidate=60";
    public static final String NO_STORE = "no-store";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean publicRead = "GET".equals(request.getMethod()) && request.getRequestURI().startsWith(PUBLIC_PREFIX);
        response.setHeader(HttpHeaders.CACHE_CONTROL, publicRead ? PUBLIC_CACHE : NO_STORE);
        chain.doFilter(request, response);
    }
}
