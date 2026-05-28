package com.infratrack.infrastructure.adapter.output;

import com.infratrack.application.port.output.UserRepository;
import com.infratrack.domain.model.User;
import com.infratrack.domain.model.Username;
import com.infratrack.infrastructure.persistence.SpringDataUserRepository;
import com.infratrack.infrastructure.persistence.UserJpaEntity;
import com.infratrack.infrastructure.persistence.UserMapper;

import java.util.Objects;
import java.util.Optional;

// Output adapter: implements the UserRepository port using JPA.
// No @Repository — wired manually in BeanConfiguration.
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springRepo;

    public JpaUserRepository(SpringDataUserRepository springRepo) {
        this.springRepo = Objects.requireNonNull(springRepo, "SpringDataUserRepository cannot be null");
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return springRepo.findByUsername(username.getValue())
                .map(UserMapper::fromJpaEntity);
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = springRepo.save(UserMapper.toJpaEntity(user));
        return UserMapper.fromJpaEntity(saved);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return springRepo.existsByUsername(username.getValue());
    }
}
