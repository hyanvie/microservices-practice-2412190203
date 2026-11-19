package com.zjgsu.hyh.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verifiesPasswordWithoutStoringPlaintext() {
        String firstHash = hasher.hash("BuyerDemo!2026");
        String secondHash = hasher.hash("BuyerDemo!2026");

        assertTrue(firstHash.startsWith("pbkdf2-sha256$210000$"));
        assertNotEquals(firstHash, secondHash);
        assertTrue(hasher.matches("BuyerDemo!2026", firstHash));
        assertFalse(hasher.matches("wrong-password", firstHash));
    }
}
