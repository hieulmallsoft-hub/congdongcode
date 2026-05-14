package com.example.codetogether.security;

import com.example.codetogether.helper.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int authMaxRequests;
    private final long authWindowMillis;
    private final int writeMaxRequests;
    private final long writeWindowMillis;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.auth.max-requests:10}") int authMaxRequests,
            @Value("${app.rate-limit.auth.window-seconds:60}") long authWindowSeconds,
            @Value("${app.rate-limit.write.max-requests:40}") int writeMaxRequests,
            @Value("${app.rate-limit.write.window-seconds:60}") long writeWindowSeconds
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.authMaxRequests = authMaxRequests;
        this.authWindowMillis = authWindowSeconds * 1000;
        this.writeMaxRequests = writeMaxRequests;
        this.writeWindowMillis = writeWindowSeconds * 1000;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LimitRule rule = ruleFor(request);
        if (!enabled || rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.name + ":" + clientIp(request);
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.compute(key, (ignored, current) -> {
            if (current == null || now >= current.resetAtMillis) {
                return new WindowCounter(1, now + rule.windowMillis);
            }
            current.count++;
            return current;
        });

        if (counter.count > rule.maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (counter.resetAtMillis - now) / 1000)));
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many requests. Please try again later.",
                    request.getRequestURI()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private LimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && path.startsWith("/api/auth/")) {
            return new LimitRule("auth", authMaxRequests, authWindowMillis);
        }
        if (isWriteMethod(method) && (path.startsWith("/api/community/") || path.startsWith("/api/users/"))) {
            return new LimitRule("write", writeMaxRequests, writeWindowMillis);
        }
        return null;
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record LimitRule(String name, int maxRequests, long windowMillis) {
    }

    private static class WindowCounter {
        private int count;
        private final long resetAtMillis;

        private WindowCounter(int count, long resetAtMillis) {
            this.count = count;
            this.resetAtMillis = resetAtMillis;
        }
    }
}
