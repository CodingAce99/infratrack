package com.infratrack.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsTest {

    @Test
    @DisplayName("of() with valid data should create Credentials and return correct values via getters")
    void of_shouldCreateCredentialsWithValidData() {
        Credentials credentials = Credentials.of("admin", "s3cr3t");
        assertEquals("admin", credentials.getUsername());
        assertEquals("s3cr3t", credentials.getPassword());
    }

    @Test
    @DisplayName("of() should throw IllegalArgumentException when username is null")
    void of_shouldThrowWhenUsernameIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Credentials.of(null, "s3cr3t"));
        assertTrue(ex.getMessage().toLowerCase().contains("username"));
    }

    @Test
    @DisplayName("of() should throw IllegalArgumentException when username is blank")
    void of_shouldThrowWhenUsernameIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Credentials.of("   ", "s3cr3t"));
        assertTrue(ex.getMessage().toLowerCase().contains("username"));
    }

    @Test
    @DisplayName("of() should throw IllegalArgumentException when password is null")
    void of_shouldThrowWhenPasswordIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Credentials.of("admin", null));
        assertTrue(ex.getMessage().toLowerCase().contains("password"));
    }

    @Test
    @DisplayName("of() should throw IllegalArgumentException when password is blank")
    void of_shouldThrowWhenPasswordIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Credentials.of("admin", "   "));
        assertTrue(ex.getMessage().toLowerCase().contains("password"));
    }

    @Test
    @DisplayName("Two Credentials with same username and password should be equal and have same hashCode")
    void equals_shouldBeTrueForSameUsernameAndPassword() {
        Credentials c1 = Credentials.of("admin", "s3cr3t");
        Credentials c2 = Credentials.of("admin", "s3cr3t");
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    @DisplayName("toString() should contain username but must NOT contain password (security requirement)")
    void toString_shouldContainUsernameAndNeverPassword() {
        Credentials credentials = Credentials.of("admin", "s3cr3t");
        String result = credentials.toString();
        assertTrue(result.contains("admin"), "toString() debe contener el username");
        assertFalse(result.contains("s3cr3t"), "toString() NO debe contener el password");
    }
}

