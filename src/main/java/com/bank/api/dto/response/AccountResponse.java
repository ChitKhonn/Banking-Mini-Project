package com.bank.api.dto.response;

import com.bank.api.enums.AccountStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String userId;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private LocalDateTime createdAt;
}
