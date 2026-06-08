package com.infratrack.application.port.output;

import com.infratrack.domain.model.User;

public interface TokenGenerator {
    String generateToken(User user);
}
