package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Holds the data typed into the "Transfer Money" form. */
@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Please select the sender")
    private String fromAccount;

    @NotBlank(message = "Please select the receiver")
    private String toAccount;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 12, fraction = 2, message = "Maximum 12 digits and 2 decimal places")
    private BigDecimal amount;

    // ticked = fail on purpose after the withdrawal (rollback test)
    private boolean simulateFailure;
}
