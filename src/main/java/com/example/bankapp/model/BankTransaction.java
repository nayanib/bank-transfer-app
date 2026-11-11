package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row for every transfer, successful or failed. Stored in the table "bank_transactions". */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 6)
    private String fromAccount;        // sender's account number

    @Column(length = 6)
    private String toAccount;          // receiver's account number

    @Column(nullable = false, precision = 14, scale = 2)   // DECIMAL(14,2) in MySQL
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)       // saved as the text 'SUCCESS' / 'FAILED'
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;        // e.g. "Transfer completed" or the error message

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public BankTransaction(String fromAccount, String toAccount, BigDecimal amount,
                           TransactionStatus status, String description) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
