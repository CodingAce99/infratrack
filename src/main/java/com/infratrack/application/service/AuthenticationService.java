package com.infratrack.application.service;

import com.infratrack.application.port.input.AuthenticateUserUseCase;
import com.infratrack.application.port.input.AuthenticationResult;
import com.infratrack.application.port.input.LoginCommand;
import com.infratrack.application.port.output.PasswordEncoder;
import com.infratrack.application.port.output.TokenGenerator;
import com.infratrack.application.port.output.UserRepository;
import com.infratrack.domain.exception.InvalidCredentialsException;
import com.infratrack.domain.exception.InvalidUsernameException;
import com.infratrack.domain.model.User;
import com.infratrack.domain.model.Username;

import java.util.Objects;

// No @Service — wired manually in BeanConfiguration with @Profile({"demo","prod"})
public class AuthenticationService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 TokenGenerator tokenGenerator) {
        this.userRepository  = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.tokenGenerator  = Objects.requireNonNull(tokenGenerator);
    }

    @Override
    public AuthenticationResult login(LoginCommand command) {

        // --- Username construction with silent catch ---

        // We catch InvalidUsernameException here to avoid leaking format rules to the caller.
        // From the outside, a malformed username and a missing user look identical: same 401,
        // same body. An attacker should not be able to tell the difference.

        Username username;
        try {
            username = new Username(command.username());
        } catch (InvalidUsernameException e) {
            throw new InvalidCredentialsException();
        }


        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        // --- Password verification ---
        //
        // Why not throw a different exception for wrong password vs user not found?
        // Same reason as above: uniform failure. An attacker who gets different errors
        // for "user not found" vs "wrong password" can enumerate valid usernames via
        // brute force. One exception for all failure modes closes that vector.

        // Note: a hardened login would also equalize response time by running a dummy hash
        // when the user doesn't exist, to prevent timing attacks. Out of scope for now.

        if (!passwordEncoder.matches(command.rawPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenGenerator.generateToken(user);
        return new AuthenticationResult(token, user.getUsername().toString(), user.getUserRole().name());
    }
}
