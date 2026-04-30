package com.trailmatch.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String refreshToken, String tokenType) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
}
