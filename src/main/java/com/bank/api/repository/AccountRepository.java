package com.bank.api.repository;

import com.bank.api.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {

    List<Account> findByUserId(String userId);

    long countByUserId(String userId);
}
