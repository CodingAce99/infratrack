package com.infratrack.domain.model;

import java.util.Objects;

public final class Credentials {

    private final String username;
    private final String password;

    private Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static Credentials of(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Credentials username cannot be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Credentials password cannot be null or blank");
        }
        return new Credentials(username, password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Credentials other)) return false;
        return Objects.equals(username, other.username)
                && Objects.equals(password, other.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return "Credentials{username='" + username + "'}";
    }
}

