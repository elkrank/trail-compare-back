package com.trailmatch.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends GenericFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getRequestURI().equals("/api/admin/auth/login") && req.getMethod().equals("POST")) {
            String key = req.getRemoteAddr();
            Bucket b = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))).build());
            if (!b.tryConsume(1)) { ((HttpServletResponse) response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); return; }
        }
        chain.doFilter(request, response);
    }
}
