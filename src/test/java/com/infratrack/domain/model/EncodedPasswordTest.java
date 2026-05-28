package com.infratrack.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EncodedPassword")
public class EncodedPasswordTest {

    // -------------------------------------------------------------------------
    // Valid construction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("constructor accepts a valid BCrypt encoded password")
    void constructor_acceptsValidBCrypt() {
         // GIVEN / WHEN
         EncodedPassword encodedPassword = new EncodedPassword("$2a$10$abcdefghijklmnopqrstuuABC123");

         // THEN
         assertEquals("$2a$10$abcdefghijklmnopqrstuuABC123", encodedPassword.getValue());
    }

    // -------------------------------------------------------------------------
    // Invalid construction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("constructor throws when value is null")
    void constructor_throwsNullPointerException_whenValueIsNull() {
        assertThrows(NullPointerException.class, () -> new EncodedPassword(null));
    }

    @Test
    @DisplayName("constructor throws when value is blank")
    void constructor_throwsIllegalArgumentException_whenValueIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new EncodedPassword(""));
    }

    @Test
    @DisplayName("two EncodedPassword instances with the same value are equal")
    void equals_returnsTrueForSameValue() {
        EncodedPassword encodedPassword1 = new EncodedPassword("password");
        EncodedPassword encodedPassword2 = new EncodedPassword("password");
        assertEquals(encodedPassword1, encodedPassword2);
    }
}
