package com.bank.api.controller;

import com.bank.api.dto.request.AmountRequest;
import com.bank.api.dto.request.TransferRequest;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.security.UserPrincipal;
import com.bank.api.service.TransactionService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Transaction Management")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Deposit", description = "USER only - cannot deposit to others' accounts")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/accounts/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable String accountId,
                                                         @Valid @RequestBody AmountRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        TransactionResponse response = transactionService.deposit(accountId, request.getAmount(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Withdraw", description = "USER only - cannot withdraw from others' accounts")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/accounts/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@PathVariable String accountId,
                                                          @Valid @RequestBody AmountRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        TransactionResponse response = transactionService.withdraw(accountId, request.getAmount(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Transfer", description = "USER only - cannot deduct money from others' accounts")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        TransactionResponse response = transactionService.transfer(
                request.getFromAccountId(), request.getToAccountId(), request.getAmount(), principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get account transactions", description = "USER/ADMIN - cannot get others' accounts without ADMIN rights")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(@PathVariable String accountId,
                                                                              @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() == com.bank.api.enums.Role.ADMIN;
        return ResponseEntity.ok(transactionService.getAccountTransactions(accountId, principal.getId(), isAdmin));
    }
}
