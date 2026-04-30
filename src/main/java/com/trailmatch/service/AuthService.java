package com.trailmatch.service;

import com.trailmatch.dto.AuthDtos.*;
import com.trailmatch.entity.AdminUser;
import com.trailmatch.exception.ApiException;
import com.trailmatch.repository.AdminUserRepository;
import com.trailmatch.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AuthService {
    private final AdminUserRepository repo; private final PasswordEncoder encoder; private final JwtService jwt;
    public TokenResponse login(LoginRequest req){
        AdminUser u = repo.findByUsername(req.username()).orElseThrow(() -> new ApiException(401, "invalid_credentials"));
        if(!encoder.matches(req.password(), u.getPasswordHash())) throw new ApiException(401, "invalid_credentials");
        return new TokenResponse(jwt.generateAccess(u.getUsername(), u.getRole()), jwt.generateRefresh(u.getUsername()), "Bearer");
    }
    public TokenResponse refresh(RefreshRequest req){
        Claims c = jwt.parse(req.refreshToken());
        if(!"refresh".equals(c.get("type", String.class))) throw new ApiException(401, "invalid_token_type");
        AdminUser u = repo.findByUsername(c.getSubject()).orElseThrow(() -> new ApiException(401, "invalid_subject"));
        return new TokenResponse(jwt.generateAccess(u.getUsername(), u.getRole()), req.refreshToken(), "Bearer");
    }
}
