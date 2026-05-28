package com.infratrack.domain.model;

import com.infratrack.domain.exception.InvalidUsernameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Username")
public class UsernameTest {

    // -------------------------------------------------------------------------
    // Valid construction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("constructor accepts a valid username")
    void constructor_acceptsValidUsername() {
        // GIVEN / WHEN
        Username username = new Username("admin");

        // THEN
        assertEquals("admin", username.getValue());
    }

    @Test
    @DisplayName("constructor accepts username with underscores and hyphens")
    void constructor_acceptsUnderscoresAndHyphens() {
        // GIVEN / WHEN / THEN
        assertDoesNotThrow(() -> new Username("my_user-01"));
    }

    @Test
    @DisplayName("constructor accepts username of exactly 3 characters")
    void constructor_acceptsMinLength() {
        assertDoesNotThrow(() -> new Username("abc"));
    }

    @Test
    @DisplayName("constructor accepts username of exactly 50 characters")
    void constructor_acceptsMaxLength() {
        String fiftyChars = "a".repeat(50);
        assertDoesNotThrow(() -> new Username(fiftyChars));
    }

    // -------------------------------------------------------------------------
    // Invalid construction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("constructor throws when value is null")
    void constructor_throwsNullPointerException_whenValueIsNull() {
        assertThrows(NullPointerException.class, () -> new Username(null));
    }

    @Test
    @DisplayName("constructor throws when value is blank")
    void constructor_throwsInvalidUsernameException_whenValueIsBlank() {
        assertThrows(InvalidUsernameException.class, () -> new Username("   "));
    }

    @Test
    @DisplayName("constructor throws when values is shorter than 3 characters")
    void constructor_throwsInvalidUsernameException_whenTooShort() {
        assertThrows(InvalidUsernameException.class, () -> new Username("ab"));
    }

    @Test
    @DisplayName("constructor throws when value is longer than 50 characters")
    void constructor_throwsInvalidUsernameException_whenTooLong() {
        String fiftyOneChars = "a".repeat(51);
        assertThrows(InvalidUsernameException.class, () -> new Username(fiftyOneChars));
    }

    @Test
    @DisplayName("constructor throws when value contains invalid characters")
    void constructor_throwsInvalidUsernameException_whenContainsInvalidCharacters() {
        assertThrows(InvalidUsernameException.class, () -> new Username("user@name"));
    }

    // -------------------------------------------------------------------------
    // Equality
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("two usernames with the same value are equal")
    void equals_returnsTrueForSameValue() {
        // GIVEN
        Username a = new Username("admin");
        Username b = new Username("admin");

        // THEN
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("two usernames with different values are not equal")
    void equals_returnsFalseForDifferentValues() {
        assertNotEquals(new Username("admin"), new Username("user"));
    }
}

