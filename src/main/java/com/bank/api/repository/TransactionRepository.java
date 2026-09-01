package com.bank.api.repository;

import com.bank.api.entity.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    @Query("{ '$or': [ { 'accountId': ?0 }, { 'fromAccountId': ?0 }, { 'toAccountId': ?0 } ] }")
    List<Transaction> findAllByAccount(String accountId);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
