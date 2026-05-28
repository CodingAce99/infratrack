package com.infratrack.application.port.output;

import com.infratrack.domain.model.EncodedPassword;

public interface PasswordEncoder {
    EncodedPassword encode(String rawPassword);
    boolean matches(String rawPassword, EncodedPassword encoded);
}
