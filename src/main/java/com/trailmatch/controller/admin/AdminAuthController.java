package com.trailmatch.controller.admin;

import com.trailmatch.dto.AuthDtos.*;
import com.trailmatch.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/auth") @RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService service;
    @PostMapping("/login") public TokenResponse login(@RequestBody @Valid LoginRequest req){ return service.login(req); }
    @PostMapping("/refresh") public TokenResponse refresh(@RequestBody @Valid RefreshRequest req){ return service.refresh(req); }
}
