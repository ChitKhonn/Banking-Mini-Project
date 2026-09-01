package com.bank.api.service;

import com.bank.api.config.RedisConfig;
import com.bank.api.dto.response.TransactionResponse;
import com.bank.api.entity.Account;
import com.bank.api.entity.Transaction;
import com.bank.api.entity.User;
import com.bank.api.enums.TransactionStatus;
import com.bank.api.enums.TransactionType;
import com.bank.api.exception.InsufficientBalanceException;
import com.bank.api.exception.ResourceNotFoundException;
import com.bank.api.exception.UnauthorizedAccessException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.TransactionRepository;
import com.bank.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountId")
    public TransactionResponse deposit(String accountId, BigDecimal amount, String requesterId) {
        Account account = getOwnedAccount(accountId, requesterId, "Cannot deposit to another user's account");

        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionType(TransactionType.DEPOSIT)
                .accountId(accountId)
                .amount(amount)
                .balanceAfter(saved.getBalance())
                .status(TransactionStatus.SUCCESS)
                .build();
        Transaction savedTxn = transactionRepository.save(txn);

        userRepository.findById(requesterId).ifPresent(owner ->
                emailService.sendDepositEmail(owner, savedTxn));

        return toResponse(savedTxn);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountId")
    public TransactionResponse withdraw(String accountId, BigDecimal amount, String requesterId) {
        Account account = getOwnedAccount(accountId, requesterId, "Cannot withdraw from another user's account");

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for withdrawal");
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account saved = accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionType(TransactionType.WITHDRAW)
                .accountId(accountId)
                .amount(amount)
                .balanceAfter(saved.getBalance())
                .status(TransactionStatus.SUCCESS)
                .build();
        Transaction savedTxn = transactionRepository.save(txn);

        userRepository.findById(requesterId).ifPresent(owner ->
                emailService.sendWithdrawEmail(owner, savedTxn));

        return toResponse(savedTxn);
    }

    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#fromAccountId"),
            @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#toAccountId")
    })
    public TransactionResponse transfer(String fromAccountId, String toAccountId, BigDecimal amount, String requesterId) {
        Account fromAccount = getOwnedAccount(fromAccountId, requesterId, "Cannot deduct money from another user's account");
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + toAccountId));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for transfer");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        Account savedFrom = accountRepository.save(fromAccount);
        Account savedTo = accountRepository.save(toAccount);

        Transaction txn = Transaction.builder()
                .transactionType(TransactionType.TRANSFER)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .fromBalanceAfter(savedFrom.getBalance())
                .toBalanceAfter(savedTo.getBalance())
                .status(TransactionStatus.SUCCESS)
                .build();
        Transaction savedTxn = transactionRepository.save(txn);

        User sender = userRepository.findById(savedFrom.getUserId()).orElse(null);
        User receiver = userRepository.findById(savedTo.getUserId()).orElse(null);
        if (sender != null && receiver != null) {
            emailService.sendTransferEmails(sender, receiver, savedTxn);
        }

        return toResponse(savedTxn);
    }

    @Cacheable(value = RedisConfig.TRANSACTIONS_CACHE, key = "#accountId")
    public List<TransactionResponse> getAccountTransactions(String accountId, String requesterId, boolean isAdmin) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!isAdmin && !account.getUserId().equals(requesterId)) {
            throw new UnauthorizedAccessException("Cannot access another user's transactions without ADMIN rights");
        }

        return transactionRepository.findAllByAccount(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Account getOwnedAccount(String accountId, String requesterId, String forbiddenMessage) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.getUserId().equals(requesterId)) {
            throw new UnauthorizedAccessException(forbiddenMessage);
        }
        return account;
    }

    private TransactionResponse toResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionType(txn.getTransactionType())
                .accountId(txn.getAccountId())
                .fromAccountId(txn.getFromAccountId())
                .toAccountId(txn.getToAccountId())
                .amount(txn.getAmount())
                .balanceAfter(txn.getBalanceAfter())
                .fromBalanceAfter(txn.getFromBalanceAfter())
                .toBalanceAfter(txn.getToBalanceAfter())
                .status(txn.getStatus())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
