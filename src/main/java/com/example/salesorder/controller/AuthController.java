package com.example.salesorder.controller;

import com.example.salesorder.config.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        // Simple authentication - in production, use proper user service
        // For demo purposes, accept any username/password and assign role based on username
        String role = "USER";
        if ("admin".equalsIgnoreCase(request.username())) {
            role = "ADMIN";
        }

        String token = jwtTokenProvider.generateToken(request.username(), role);
        return ResponseEntity.ok(Map.of("token", token, "username", request.username(), "role", role));
    }

    public record LoginRequest(String username, String password) {}
}

