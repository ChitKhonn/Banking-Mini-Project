package com.bank.api.repository;

import com.bank.api.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': 'DELETED' } }")
    long softDelete(String id);
}
