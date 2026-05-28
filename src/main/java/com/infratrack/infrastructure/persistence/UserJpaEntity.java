package com.infratrack.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Stores the BCrypt hash — no EncryptedStringConverter here.
    // BCrypt hashes are already irreversible; AES encryption on top adds no security value.
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role;

    // Required by JPA — Hibernate needs to instantiate the object before populating fields.
    protected UserJpaEntity() {}

    public UserJpaEntity(String id, String username, String passwordHash, String role) {
        this.id           = id;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    public String getId()           { return id; }
    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole()         { return role; }
}
