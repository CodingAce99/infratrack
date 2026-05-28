package com.infratrack.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User")
class UserTest {

    private final Username username = new Username("admin");
    private final EncodedPassword password = new EncodedPassword("$2a$10$abc");
    private final UserRole role = UserRole.ADMIN;

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create() generates a non-null UserId")
    void create_generatesNonNullId() {
        // GIVEN / WHEN
        User user = User.create(username, password, role);
        // THEN
        assertNotNull(user.getId());
    }

    @Test
    @DisplayName("create() preserves username, password and role")
    void create_preservesAllFields() {
        // GIVEN / WHEN
        User user = User.create(username, password, role);
        // THEN
        assertAll(
                () -> assertEquals(username, user.getUsername()),
                () -> assertEquals(password, user.getPassword()),
                () -> assertEquals(role, user.getUserRole())
        );
    }

    @Test
    @DisplayName("create() throws NullPointerException when username is null")
    void create_throwsNullPointerException_whenUsernameIsNull() {
        assertThrows(NullPointerException.class, () ->
                User.create(null, password, role));
    }

    // -------------------------------------------------------------------------
    // reconstitute()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("reconstitute() preserves all fields including the provided UserId")
    void reconstitute_preservesAllFields() {
        // GIVEN
        UserId id = UserId.generate();
        // WHEN
        User user = User.reconstitute(id, username, password, role);
        // THEN
        assertAll(
                () -> assertEquals(id, user.getId()),
                () -> assertEquals(username, user.getUsername()),
                () -> assertEquals(password, user.getPassword()),
                () -> assertEquals(role, user.getUserRole())
        );
    }

    // -------------------------------------------------------------------------
    // Equality
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("two users with the same UserId are equal")
    void equals_returnsTrueForSameId() {
        // GIVEN
        UserId id = UserId.generate();
        User a = User.reconstitute(id, new Username("admin"), password, role);
        User b = User.reconstitute(id, new Username("viewer"), password, UserRole.VIEWER);
        // THEN
        assertEquals(a, b);
    }

    @Test
    @DisplayName("two users with different UserIds are not equal")
    void equals_returnsFalseForDifferentIds() {
        User a = User.create(username, password, role);
        User b = User.create(username, password, role);
        assertNotEquals(a, b);
    }
}