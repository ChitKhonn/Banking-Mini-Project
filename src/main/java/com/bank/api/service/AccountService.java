package com.bank.api.service;

import com.bank.api.config.RedisConfig;
import com.bank.api.dto.request.CreateAccountRequest;
import com.bank.api.dto.response.AccountResponse;
import com.bank.api.entity.Account;
import com.bank.api.enums.AccountStatus;
import com.bank.api.exception.DuplicateResourceException;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.exception.UnauthorizedAccessException;
import com.bank.api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private static final String BANK_CODE = "BNK";
    private static final String BRANCH_CODE = "001";
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() % 100000);

    @CacheEvict(value = RedisConfig.ACCOUNTS_BY_USER_CACHE, key = "#request.userId")
    public AccountResponse create(CreateAccountRequest request) {
        if (accountRepository.countByUserId(request.getUserId()) > 0) {
            throw new DuplicateResourceException(
                    "This customer already has a bank account. Only one account per customer is allowed.");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .userId(request.getUserId())
                .balance(request.getOpeningBalance())
                .currency(request.getCurrency())
                .status(AccountStatus.ACTIVE)
                .build();

        return toResponse(accountRepository.save(account));
    }

    @Cacheable(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountId")
    @PostAuthorize("returnObject.userId == authentication.principal.id or hasRole('ADMIN')")
    public AccountResponse getById(String accountId) {
        Account account = findOrThrow(accountId);
        return toResponse(account);
    }

    @Cacheable(value = RedisConfig.ACCOUNTS_BY_USER_CACHE, key = "#targetUserId")
    @PostAuthorize("hasRole('ADMIN') or #targetUserId == authentication.principal.id")
    public List<AccountResponse> listByUser(String targetUserId) {
        return accountRepository.findByUserId(targetUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountId"),
    })
    @PostAuthorize("returnObject.userId == authentication.principal.id or hasRole('ADMIN')")
    public AccountResponse updateStatus(String accountId, AccountStatus newStatus) {
        Account account = findOrThrow(accountId);
        account.setStatus(newStatus);
        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountId")
    public void delete(String accountId) {
        Account account = findOrThrow(accountId);
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    Account findOrThrow(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    private String generateAccountNumber() {
        return BANK_CODE + "-" + BRANCH_CODE + "-" + String.format("%08d", sequence.incrementAndGet());
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
