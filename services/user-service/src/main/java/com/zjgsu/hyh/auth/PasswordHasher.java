package com.zjgsu.hyh.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

/** PBKDF2-HMAC-SHA256 password hashing with per-password random salts. */
@Component
public class PasswordHasher {
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    public boolean matches(String password, String encoded) {
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Password hashing is unavailable", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
