package com.fudn.customer_service.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String role,
        Long userId,
        String email,
        String fullName
) {
}