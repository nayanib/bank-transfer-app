package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    // 1. All transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.id DESC")
    List<BankTransaction> findAllLatest();

    // 2. Transactions where the account is the sender OR the receiver
    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :account OR t.toAccount = :account ORDER BY t.id DESC")
    List<BankTransaction> findHistory(@Param("account") String account);

    // 3. Transactions with one status (SUCCESS or FAILED), newest first
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.id DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    // 4. Number of transactions with one status (for the dashboard)
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    // 5. Successful transfers above a given amount, biggest first
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status AND t.amount > :min " +
           "AND t.fromAccount IS NOT NULL AND t.toAccount IS NOT NULL ORDER BY t.amount DESC")
    List<BankTransaction> findLargeTransfers(@Param("min") BigDecimal min,
                                             @Param("status") TransactionStatus status);

    // 6. Total amount sent by an account (successful transfers, fees included)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t WHERE t.status = :status " +
           "AND t.fromAccount = :account AND t.toAccount IS NOT NULL")
    BigDecimal getTotalSent(@Param("account") String account,
                            @Param("status") TransactionStatus status);
}
