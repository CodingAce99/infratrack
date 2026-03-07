package com.infratrack.infrastructure.adapter.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateIpAddressRequest(
        @NotBlank(message = "IP address is required")
        @Pattern(
                regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$",
                message = "Invalid IP address format"
        )
        String ipAddress
) {
}
