package com.shady.landing.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Builds {@link ApiError}s and writes them from filters, where the MVC exception handler does not apply. */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ApiError build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, List.of());
    }

    public ApiError build(HttpStatus status, String message, HttpServletRequest request,
                          List<ApiError.FieldViolation> fieldErrors) {
        return new ApiError(Instant.now(clock), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fieldErrors);
    }

    public void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(response.getOutputStream(), build(status, message, request));
    }
}
