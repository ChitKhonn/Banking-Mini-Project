package com.bank.api.controller;

import com.bank.api.dto.request.UpdateUserRequest;
import com.bank.api.dto.response.UserResponse;
import com.bank.api.security.UserPrincipal;
import com.bank.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get customer by ID", description = "USER/ADMIN - customers can access their own data only")
    @PreAuthorize("hasAnyRole('USER','ADMIN')") // implemnt on security config
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(userService.getById(id, principal.getId(), isAdmin));
    }

    @Operation(summary = "List all customers", description = "ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> listAll() {
        return ResponseEntity.ok(userService.listAll());
    }

    @Operation(summary = "Update customer", description = "USER/ADMIN - cannot update others' data without ADMIN rights")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable String id,
                                                @Valid @RequestBody UpdateUserRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(userService.update(id, request, principal.getId(), isAdmin));
    }

    @Operation(summary = "Delete customer", description = "ADMIN only - strictly, no self-delete by USER")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getUser().getRole() == com.bank.api.enums.Role.ADMIN;
    }
}
