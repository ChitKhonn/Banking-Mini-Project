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
    public AccountResponse getById(String accountId, String requesterId, boolean isAdmin) {
        Account account = findOrThrow(accountId);

        if (!isAdmin && !account.getUserId().equals(requesterId)) {
            throw new UnauthorizedAccessException("Cannot access another user's account without ADMIN rights");
        }

        return toResponse(account);
    }

    @Cacheable(value = RedisConfig.ACCOUNTS_BY_USER_CACHE, key = "#targetUserId")
    public List<AccountResponse> listByUser(String targetUserId, String requesterId, boolean isAdmin) {
        if (!isAdmin && !targetUserId.equals(requesterId)) {
            throw new UnauthorizedAccessException("Cannot list another user's accounts without ADMIN rights");
        }

        return accountRepository.findByUserId(targetUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountId"),
    })
    public AccountResponse updateStatus(String accountId, AccountStatus newStatus, String requesterId, boolean isAdmin) {
        Account account = findOrThrow(accountId);

        if (!isAdmin && !account.getUserId().equals(requesterId)) {
            throw new UnauthorizedAccessException("Cannot update another user's account without ADMIN rights");
        }

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
