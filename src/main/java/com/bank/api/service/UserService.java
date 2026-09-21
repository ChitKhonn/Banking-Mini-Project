package com.bank.api.service;

import com.bank.api.dto.request.UpdateUserRequest;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.entity.User;
import com.bank.api.exception.EmailDuplicateException;
import com.bank.api.exception.UserNotFoundException;
import com.bank.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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

    @PostAuthorize("returnObject.id == authentication.principal.id or hasRole('ADMIN')")
    public UserResponse update(String targetUserId, UpdateUserRequest request) {
        User user = findOrThrow(targetUserId);

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());

        try {
            return toResponse(userRepository.save(user));
        } catch (DuplicateKeyException ex) {
            throw new EmailDuplicateException("Email already in use: " + request.getEmail());
        }
    }

    public void delete(String targetUserId) {
        long modified = userRepository.softDelete(targetUserId);
        if (modified == 0) throw new UserNotFoundException("User not found: " + targetUserId);
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
