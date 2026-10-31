package com.zjgsu.hyh.service;

import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Set<String> userIds = Set.of("buyer-1001", "seller-1001", "seller-1002");

    public boolean exists(String userId) {
        return userIds.contains(userId);
    }
}
