package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A bank customer with one account. Stored in the table "customers". */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name can have at most 100 characters")
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6}", message = "Account number must be exactly 6 digits")
    @Column(nullable = false, unique = true, length = 6)
    private String accountNumber;

    @NotNull(message = "Opening balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Maximum 12 digits and 2 decimal places")
    @Column(nullable = false, precision = 14, scale = 2)   // DECIMAL(14,2) in MySQL
    private BigDecimal balance;

    public Customer(String fullName, String email, String accountNumber, BigDecimal balance) {
        this.fullName = fullName;
        this.email = email;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
}
