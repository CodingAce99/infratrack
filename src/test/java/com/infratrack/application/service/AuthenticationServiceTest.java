package com.infratrack.application.service;

import com.infratrack.application.port.input.AuthenticationResult;
import com.infratrack.application.port.input.LoginCommand;
import com.infratrack.application.port.output.PasswordEncoder;
import com.infratrack.application.port.output.TokenGenerator;
import com.infratrack.application.port.output.UserRepository;
import com.infratrack.domain.exception.InvalidCredentialsException;
import com.infratrack.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenGenerator tokenGenerator;

    private AuthenticationService service;

    // Why not @InjectMocks? Because AuthenticationService is wired manually in
    // BeanConfiguration — it has no @Service annotation. @InjectMocks would work
    // technically, but instantiating it explicitly in @BeforeEach, mirrors how
    // Spring actually builds it and makes the constructor contract visible in tests.

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, passwordEncoder, tokenGenerator);
    }

    // Shared test fixture -- a valid user reconstituted from persistence
    private User validUser() {
        return User.reconstitute(
                UserId.generate(),
                new Username("admin"),
                new EncodedPassword("$2a$10$hashedpassword"),
                UserRole.ADMIN
        );
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns populated AuthenticationResult on valid credentials")
        void login_returnsAuthenticationResult_onValidCredentials() {
            // GIVEN
            User user = validUser();
            LoginCommand command = new LoginCommand("admin", "admin123");

            when(userRepository.findByUsername(new Username("admin")))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("admin123", user.getPassword()))
                    .thenReturn(true);
            when(tokenGenerator.generateToken(user))
                    .thenReturn("mocked.jwt.token");

            // WHEN
            AuthenticationResult result = service.login(command);

            // THEN
            assertAll(
                    () -> assertEquals("mocked.jwt.token", result.token()),
                    () -> assertEquals("admin", result.username()),
                    () -> assertEquals("ADMIN", result.role())
            );
            verify(tokenGenerator, times(1)).generateToken(user);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when user not found")
        void login_throwsInvalidCredentialsException_whenUserNotFound() {
            // GIVEN
            LoginCommand command = new LoginCommand("admin", "admin123");

            when(userRepository.findByUsername(new Username("admin")))
                    .thenReturn(Optional.empty());

            // WHEN + THEN
            assertThrows(InvalidCredentialsException.class, () -> service.login(command));

        }

        @Test
        @DisplayName("throws InvalidCredentialsException when password does not match")
        void login_throwsInvalidCredentialsException_whenPasswordDoesNotMatch() {
            // GIVEN
            User user = validUser();
            LoginCommand command = new LoginCommand("admin", "admin123");

            when(userRepository.findByUsername(new Username("admin")))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("admin123", user.getPassword()))
                    .thenReturn(false);

            // WHEN + THEN
            assertThrows(InvalidCredentialsException.class, () -> service.login(command));
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when username is invalid")
        void login_throwsInvalidCredentialsException_whenUsernameIsInvalid() {
            // GIVEN
            LoginCommand command = new LoginCommand("a!", "admin123");

            //WHEN + THEN
            assertThrows(InvalidCredentialsException.class, () -> service.login(command));
        }

        @Test
        @DisplayName("verifies that passwordEncoder.matches() is called with correct arguments")
        void login_verifiesPasswordEncoderMatchesCalledWithCorrectArguments() {
            // GIVEN
            User user = validUser();
            LoginCommand command = new LoginCommand("admin", "admin123");

            when(userRepository.findByUsername(new Username("admin")))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("admin123", user.getPassword()))
                    .thenReturn(true);

            // WHEN
            service.login(command);

            // THEN
            verify(passwordEncoder, times(1)).matches("admin123", user.getPassword());
        }
    }
}
