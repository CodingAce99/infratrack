package com.infratrack.infrastructure.adapter.input.dto;

import com.infratrack.application.port.input.AuthenticationResult;
import com.infratrack.application.port.input.LoginCommand;

public class AuthDtoMapper {

    private AuthDtoMapper() {
        // Utility class - not instantiable
    }

    public static LoginCommand toCommand(LoginRequest loginRequest) {
        return new LoginCommand(loginRequest.username(), loginRequest.password());
    }

    public static LoginResponse toResponse(AuthenticationResult authResult) {
        return new LoginResponse(
                authResult.token(),
                "Bearer",
                authResult.username(),
                authResult.role()
        );
    }
}
