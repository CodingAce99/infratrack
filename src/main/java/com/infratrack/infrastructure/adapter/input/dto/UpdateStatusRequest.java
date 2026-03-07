package com.infratrack.infrastructure.adapter.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(
                regexp = "ACTIVE|INACTIVE|MAINTENANCE",
                message = "Status must be ACTIVE, INACTIVE or MAINTENANCE"
        )
        String status
) {
}
