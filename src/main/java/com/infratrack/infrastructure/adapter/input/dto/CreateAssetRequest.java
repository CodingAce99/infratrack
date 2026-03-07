package com.infratrack.infrastructure.adapter.input.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAssetRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Type is required")
        @Pattern(
                regexp = "SERVER|ROUTER|IOT_DEVICE",
                message = "Type must be SERVER, ROUTER or IOT_DEVICE"
        )
        String type,

        @NotBlank(message = "IP address is required")
        @Pattern(
                regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$",
                message = "Invalid IP address format"
        )
        String ipAddress,

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
