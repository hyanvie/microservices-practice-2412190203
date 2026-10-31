package com.zjgsu.hyh.service;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Set<String> userIds = Set.of("buyer-1001", "seller-1001", "seller-1002");
    private final JdbcTemplate jdbcTemplate;

    public UserService() {
        this.jdbcTemplate = null;
    }

    @Autowired
    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(String userId) {
        if (jdbcTemplate != null) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ?",
                    Integer.class,
                    userId) > 0;
        }
        return userIds.contains(userId);
    }
}
