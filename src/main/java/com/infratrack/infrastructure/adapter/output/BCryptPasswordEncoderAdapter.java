package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.PasswordEncoder;
import com.infratrack.domain.model.EncodedPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Output adapter: implements the PasswordEncoder port using BCrypt
// No @Component -- wired manually in BeanConfiguration
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder();

    @Override
    public EncodedPassword encode(String rawPassword) {
        return new EncodedPassword(bCrypt.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, EncodedPassword encoded) {
        return new BCryptPasswordEncoder().matches(rawPassword, encoded.getValue());
    }
}
