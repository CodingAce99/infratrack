package com.infratrack.infrastructure.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts String fields
 * using AES-256-GCM before storing them in the database.
 *
 * <p>Format stored in DB: Base64( IV (12 bytes) || ciphertext )
 *
 * <p>The encryption key must be provided as a Base64-encoded 32-byte value
 * via the environment variable {@code INFRATRACK_ENCRYPTION_KEY}, mapped to
 * the Spring property {@code infratrack.encryption.key}.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM       = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM   = "AES";
    private static final int    GCM_TAG_LENGTH  = 128; // bits
    private static final int    GCM_IV_LENGTH   = 12;  // bytes

    private final SecretKey secretKey;

    public EncryptedStringConverter(@Value("${infratrack.encryption.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    /**
     * Encrypts the given plaintext attribute value before persisting it.
     *
     * @param attribute the plaintext String; {@code null} is stored as {@code null}
     * @return Base64-encoded string containing the random IV followed by the ciphertext
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes());

            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting field value", e);
        }
    }

    /**
     * Decrypts the Base64-encoded database value back to the original plaintext.
     *
     * @param dbData the Base64-encoded IV + ciphertext; {@code null} is returned as {@code null}
     * @return the original plaintext String
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            byte[] iv         = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting field value", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}

