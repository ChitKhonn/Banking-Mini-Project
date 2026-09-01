package com.bank.api.service;

import com.bank.api.dto.request.UpdateUserRequest;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.entity.User;
import com.bank.api.enums.UserStatus;
import com.bank.api.exception.DuplicateResourceException;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.exception.UnauthorizedAccessException;
import com.bank.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getById(String targetUserId, String requesterId, boolean isAdmin) {
        User user = findOrThrow(targetUserId);

        // 25 , 27 use postAuthorize
        if (!isAdmin && !targetUserId.equals(requesterId)) {
            throw new UnauthorizedAccessException("You can only access your own data");
        }

        return toResponse(user);
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse update(String targetUserId, UpdateUserRequest request, String requesterId, boolean isAdmin) {
        User user = findOrThrow(targetUserId);

        if (!isAdmin && !targetUserId.equals(requesterId)) {
            throw new UnauthorizedAccessException("Cannot update another user's data without ADMIN rights");
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail()); //emilduplicatexception
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());

        return toResponse(userRepository.save(user)); // repo db exception
    }

    public void delete(String targetUserId) {
        User user = findOrThrow(targetUserId); // no need this line
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    private User findOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
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
