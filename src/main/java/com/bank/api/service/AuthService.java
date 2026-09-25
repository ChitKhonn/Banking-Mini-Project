package com.bank.api.service;

import com.bank.api.dto.request.LoginRequest;
import com.bank.api.dto.request.RegisterRequest;
import com.bank.api.dto.response.LoginResponse;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.entity.User;
import com.bank.api.enums.UserStatus;
import com.bank.api.exception.DuplicateResourceException;
import com.bank.api.exception.EmailDuplicateException;
import com.bank.api.repository.UserRepository;
import com.bank.api.security.JwtService;
import com.bank.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtService.generateToken(principal.getUsername(), principal.getUser().getRole().name());

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpirationSeconds())
                .build();
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicateException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
