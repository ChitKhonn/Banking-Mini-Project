package com.bank.api.dto.response;

import com.bank.api.enums.TransactionStatus;
import com.bank.api.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String id;
    private TransactionType transactionType;
    private String accountId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}
