package com.infratrack.infrastructure.persistence;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, String> {
     Optional<UserJpaEntity> findByUsername(String username);
     boolean existsByUsername(String username);

}
