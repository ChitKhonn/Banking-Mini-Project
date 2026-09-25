package com.bank.api.repository;

import com.bank.api.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface AccountRepository extends MongoRepository<Account, String> {

    List<Account> findByUserId(String userId);

    boolean existsByUserId(String userId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': 'CLOSED' } }")
    long softClose(String id);
}
