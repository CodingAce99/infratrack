package com.infratrack.infrastructure.adapter.input.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCredentialsRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
