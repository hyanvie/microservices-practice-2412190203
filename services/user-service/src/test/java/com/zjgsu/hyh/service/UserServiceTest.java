package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserServiceTest {

    @Test
    void checksKnownUserIds() {
        UserService service = new UserService();

        assertTrue(service.exists("buyer-1001"));
        assertFalse(service.exists("unknown-user"));
    }
}
