package com.zjgsu.hyh.controller;

import com.zjgsu.hyh.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<Void> exists(@PathVariable String userId) {
        return userService.exists(userId) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
