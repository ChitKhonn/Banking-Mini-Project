package com.bank.api.controller;

import com.bank.api.dto.request.CreateAccountRequest;
import com.bank.api.dto.response.AccountResponse;
import com.bank.api.enums.AccountStatus;
import com.bank.api.security.UserPrincipal;
import com.bank.api.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Account Management")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Create bank account", description = "ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get account by ID", description = "USER/ADMIN - cannot access others' account without ADMIN rights")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable String id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getById(id, principal.getId(), isAdmin(principal)));
    }

    @Operation(summary = "List accounts by customer", description = "USER/ADMIN - cannot list others' accounts without ADMIN rights")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/customer/{userId}")
    public ResponseEntity<List<AccountResponse>> listByCustomer(@PathVariable String userId,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.listByUser(userId, principal.getId(), isAdmin(principal)));
    }

    @Operation(summary = "Update account status", description = "USER/ADMIN - cannot update others' accounts without ADMIN rights")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(@PathVariable String id,
                                                          @RequestParam AccountStatus status,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.updateStatus(id, status, principal.getId(), isAdmin(principal)));
    }

    @Operation(summary = "Delete account", description = "ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getUser().getRole() == com.bank.api.enums.Role.ADMIN;
    }
}
