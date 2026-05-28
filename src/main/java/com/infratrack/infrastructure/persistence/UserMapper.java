package com.infratrack.infrastructure.persistence;

import com.infratrack.domain.model.*;

public final class UserMapper {

    private UserMapper() {}

    public static UserJpaEntity toJpaEntity(User user) {
        return new UserJpaEntity(
                user.getId().toString(),
                user.getUsername().toString(),
                user.getPassword().toString(),
                user.getUserRole().name()
        );
    }

    public static User fromJpaEntity(UserJpaEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                new Username(entity.getUsername()),
                new EncodedPassword(entity.getPasswordHash()),
                UserRole.valueOf(entity.getRole())
        );
    }
}
