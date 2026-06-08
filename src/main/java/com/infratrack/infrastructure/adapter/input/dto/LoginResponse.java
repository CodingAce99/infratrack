package com.infratrack.infrastructure.adapter.input.dto;

public record LoginResponse(
        String token,
        String type,
        String username,
        String role
) {
}
