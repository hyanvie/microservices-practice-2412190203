package com.zjgsu.hyh.auth;

import com.zjgsu.hyh.auth.AuthService.InvalidCredentialsException;
import com.zjgsu.hyh.auth.AuthService.LoginResult;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResult login(@RequestBody LoginRequest request) {
        if (request.userId() == null || request.password() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId and password are required");
        }
        try {
            return authService.login(request.userId(), request.password());
        } catch (InvalidCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
    }

    @GetMapping("/buyer/profile")
    public Map<String, String> buyerProfile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("userId", jwt.getSubject(), "role", "BUYER");
    }

    @GetMapping("/seller/profile")
    public Map<String, String> sellerProfile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("userId", jwt.getSubject(), "role", "SELLER");
    }

    public record LoginRequest(String userId, String password) { }
}
