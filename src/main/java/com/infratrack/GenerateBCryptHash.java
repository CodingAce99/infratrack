package com.infratrack;

// Temporary utility — run once, copy hashes to V3, then DELETE this file
public class GenerateBCryptHash {
    public static void main(String[] args) {
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        System.out.println("admin123  → " + encoder.encode("admin123"));
        System.out.println("viewer123 → " + encoder.encode("viewer123"));
    }
}
