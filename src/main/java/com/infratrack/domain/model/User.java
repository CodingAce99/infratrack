package com.infratrack.domain.model;

import java.util.Objects;

public final class User {

    private final UserId id;
    private final Username username;
    private final EncodedPassword password;
    private final UserRole role;

    private User(UserId id, Username username, EncodedPassword password, UserRole role) {
        this.id =       Objects.requireNonNull(id, "User ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.password = Objects.requireNonNull(password, "Password cannot be null");
        this.role =     Objects.requireNonNull(role, "UserRole cannot be null");
    }

    // --- Factory Methods ---
    // Called when creating a brand-new user (e.g. from a future user-creation endpoint).
    // Generates a fresh UserId automatically.
    public static User create(Username username, EncodedPassword password, UserRole role) {
        return new User(UserId.generate(), username, password, role);
    }

    // Called when rebuilding a User from the database.
    // Accepts the existing UserId — does NOT generate a new one.
    public static User reconstitute(UserId id, Username username, EncodedPassword password, UserRole role) {
        return new User(id, username, password, role);
    }

    public UserId getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public EncodedPassword getPassword() {
        return password;
    }

    public UserRole getUserRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
