package com.bank.api.controller;

import com.bank.api.dto.request.LoginRequest;
import com.bank.api.dto.request.RegisterRequest;
import com.bank.api.dto.response.LoginResponse;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login and user registration")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with email/password", description = "Public endpoint, returns JWT")
    @SecurityRequirements // overrides global bearerAuth requirement - this endpoint is public
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Register new user", description = "ADMIN only - creates a new USER or ADMIN account")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}