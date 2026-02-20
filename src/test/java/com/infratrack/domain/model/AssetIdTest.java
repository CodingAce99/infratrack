package com.infratrack.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssetIdTest {

    @Test
    @DisplayName("generate() should create a valid, non-null AssetId")
    void generate_shouldCreateValidAssetId() {
        AssetId id = AssetId.generate();
        assertNotNull(id);
        assertNotNull(id.getValue());
    }

    @Test
    @DisplayName("generate() should create unique IDs each time")
    void generate_shouldCreateUniqueIds() {
        AssetId id1 = AssetId.generate();
        AssetId id2 = AssetId.generate();
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("of(UUID) should wrap the given UUID correctly")
    void ofUuid_shouldWrapCorrectly() {
        UUID uuid = UUID.randomUUID();
        AssetId id = AssetId.of(uuid);
        assertEquals(uuid, id.getValue());
    }

    @Test
    @DisplayName("of(String) should parse a valid UUID string")
    void ofString_shouldParseValidUuid() {
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        AssetId id = AssetId.of(uuidString);
        assertEquals(uuidString, id.toString());
    }

    @Test
    @DisplayName("of(String) should throw for an invalid UUID string")
    void ofString_shouldThrowForInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> AssetId.of("not-a-valid-uuid"));
    }

    @Test
    @DisplayName("of(UUID) should throw for null input")
    void ofUuid_shouldThrowForNull() {
        assertThrows(NullPointerException.class,
                () -> AssetId.of((UUID) null));
    }

    @Test
    @DisplayName("of(String) should throw NullPointerException for null input")
    void ofString_shouldThrowForNull() {
        assertThrows(NullPointerException.class,
                () -> AssetId.of((String) null));
    }

    @Test
    @DisplayName("Two AssetIds with the same UUID should be equal")
    void equals_shouldBeTrueForSameUuid() {
        UUID uuid = UUID.randomUUID();
        AssetId id1 = AssetId.of(uuid);
        AssetId id2 = AssetId.of(uuid);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("toString() should return the UUID string representation")
    void toString_shouldReturnUuidString() {
        UUID uuid = UUID.randomUUID();
        AssetId id = AssetId.of(uuid);
        assertEquals(uuid.toString(), id.toString());
    }
}