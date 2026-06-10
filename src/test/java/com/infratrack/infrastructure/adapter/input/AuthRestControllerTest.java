package com.infratrack.infrastructure.adapter.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infratrack.application.port.input.AuthenticateUserUseCase;
import com.infratrack.application.port.input.AuthenticationResult;
import com.infratrack.domain.exception.InvalidCredentialsException;
import com.infratrack.domain.model.*;
import com.infratrack.infrastructure.adapter.input.dto.LoginRequest;
import com.infratrack.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("AuthRestController")
@WebMvcTest(AuthRestController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("demo")
class AuthRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticateUserUseCase useCase;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {
        @Test
        @DisplayName("valid request, 200 with body token")
        void valid_request_200_with_body_token() throws Exception {
            // GIVEN
            LoginRequest request = new LoginRequest("testUser", "password");
            AuthenticationResult result = new AuthenticationResult("valid.jwt.token", "testUser", "ADMIN");
            when(useCase.login(any())).thenReturn(result);

            // WHEN + THEN
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("valid.jwt.token"));
        }

        @Test
        @DisplayName("useCase throws InvalidCrentialsException, 401")
        void useCase_throws_InvalidCredentialsException_401() throws Exception {
            // GIVEN
            LoginRequest request = new LoginRequest("testUser", "wrongPassword");
            when(useCase.login(any())).thenThrow(new InvalidCredentialsException());

            // WHEN + THEN
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Blank Username or password, 400")
        void blank_username_or_password_400() throws Exception {
            // GIVEN
            LoginRequest request = new LoginRequest("testUser", "");

            // WHEN + THEN
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
