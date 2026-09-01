package com.bank.api.entity;

import com.bank.api.enums.TransactionStatus;
import com.bank.api.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    private TransactionType transactionType;

    @Indexed
    private String accountId;      // used for DEPOSIT / WITHDRAW

    private String fromAccountId;
    private String toAccountId;

    private BigDecimal amount;

    private BigDecimal balanceAfter;      // DEPOSIT / WITHDRAW only
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;

    private TransactionStatus status;

    @Indexed
    @CreatedDate
    private LocalDateTime createdAt; // queried by @Scheduled cleanup job (3-day retention), not TTL
}
