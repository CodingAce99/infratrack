package com.infratrack.infrastructure.adapter.input;

import com.infratrack.application.port.input.AuthenticateUserUseCase;
import com.infratrack.application.port.input.AuthenticationResult;
import com.infratrack.infrastructure.adapter.input.dto.AuthDtoMapper;
import com.infratrack.infrastructure.adapter.input.dto.LoginRequest;
import com.infratrack.infrastructure.adapter.input.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@Profile({"demo", "prod"})
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private final AuthenticateUserUseCase useCase;

    // Constructor injection
    public AuthRestController(AuthenticateUserUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "AuthenticateUserUseCase cannot be null");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthenticationResult result = useCase.login(AuthDtoMapper.toCommand(loginRequest));
        return ResponseEntity.status(HttpStatus.OK)
                .body(AuthDtoMapper.toResponse(result));
    }
}
