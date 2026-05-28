package com.infratrack.application.port.output;

import com.infratrack.domain.model.User;
import com.infratrack.domain.model.Username;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(Username username);
    User save(User user);
    boolean existsByUsername(Username username);
}
