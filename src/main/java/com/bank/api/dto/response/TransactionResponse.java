package com.bank.api.dto.response;

import com.bank.api.enums.TransactionStatus;
import com.bank.api.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
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
