package com.bank.api;

import com.bank.api.entity.User;
import com.bank.api.enums.Role;
import com.bank.api.enums.UserStatus;
import com.bank.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableMongoAuditing
@EnableRetry
@RequiredArgsConstructor
public class BankingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApiApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            final String adminEmail = "admin@bank.com";
            if (userRepository.existsByEmail(adminEmail)) {
                log.info("Seed admin already exists, skipping seeding.");
                return;
            }
            userRepository.save(User.builder()
                    .name("System Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@12345"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build());
            log.info("Seeded initial ADMIN user: {}", adminEmail);
        };
    }
}
