package edu.fpt.sba301.bookstore.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> SKIP_PATH_PREFIXES = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator"
    );

    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"(?:password|currentPassword|newPassword|refreshToken|accessToken)\"\\s*:\\s*\")([^\"\\\\]*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER_HEADER = Pattern.compile(
            "(Authorization\\s*:\\s*Bearer\\s+)(\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startedAt = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 8192);

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info(
                    "{} {} -> {} ({} ms)",
                    wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );

            String body = readBody(wrappedRequest);
            if (!body.isBlank()) {
                log.debug("Request body: {}", redactSensitive(body));
            }

            String authorization = wrappedRequest.getHeader("Authorization");
            if (authorization != null && !authorization.isBlank()) {
                log.debug("Authorization: {}", redactBearer(authorization));
            }
        }
    }

    private String readBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    static String redactSensitive(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return SENSITIVE_JSON_FIELD.matcher(value).replaceAll("$1[REDACTED]$3");
    }

    static String redactBearer(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return authorization;
        }
        return BEARER_HEADER.matcher(authorization).replaceAll("$1[REDACTED]");
    }
}
