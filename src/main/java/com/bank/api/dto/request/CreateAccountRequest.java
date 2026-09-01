package com.bank.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    private String currency = "MMK";
}
