package com.trailmatch.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component @RequiredArgsConstructor
public class JwtAuthFilter extends GenericFilter {
    private final JwtService jwtService;
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (!req.getRequestURI().startsWith("/api/admin/")) {
            chain.doFilter(request, response);
            return;
        }
        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims c = jwtService.parse(auth.substring(7));
                if ("access".equals(c.get("type", String.class))) {
                    var authorities = List.of(new SimpleGrantedAuthority(c.get("role", String.class)));
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(c.getSubject(), null, authorities));
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
