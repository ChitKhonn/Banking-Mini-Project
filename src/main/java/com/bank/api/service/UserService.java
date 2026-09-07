package com.bank.api.service;

import com.bank.api.dto.request.UpdateUserRequest;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.entity.User;
import com.bank.api.enums.UserStatus;
import com.bank.api.exception.EmailDuplicateException;
import com.bank.api.exception.UserNotFoundException;
import com.bank.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @PostAuthorize("returnObject.id == authentication.principal.id or hasRole('ADMIN')")
    public UserResponse getById(String targetUserId) {
        User user = findOrThrow(targetUserId);
        return toResponse(user);
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PostAuthorize("returnObject.id == authentication.principal.id or hasRole('ADMIN')")
    public UserResponse update(String targetUserId, UpdateUserRequest request) {
        User user = findOrThrow(targetUserId);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicateException("Email already in use: " + request.getEmail());
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());

        return toResponse(userRepository.save(user));
    }

    public void delete(String targetUserId) {
        User user = findOrThrow(targetUserId);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    private User findOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
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
