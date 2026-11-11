package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 1. Find one customer by account number
    @Query("SELECT c FROM Customer c WHERE c.accountNumber = :accountNumber")
    Optional<Customer> getByAccountNumber(@Param("accountNumber") String accountNumber);

    // 2. Check if an account number or email is already used (0 = not used)
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.accountNumber = :accountNumber")
    long countAccounts(@Param("accountNumber") String accountNumber);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.email = :email")
    long countEmails(@Param("email") String email);

    // 3. All customers sorted by name
    @Query("SELECT c FROM Customer c ORDER BY c.fullName")
    List<Customer> findAllOrdered();

    // 4. Search customers by name (not case sensitive)
    @Query("SELECT c FROM Customer c WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.fullName")
    List<Customer> searchByName(@Param("keyword") String keyword);

    // 5. Total money of all customers (null if there are no customers)
    @Query("SELECT SUM(c.balance) FROM Customer c")
    BigDecimal getTotalBalance();

    // 6. Native SQL: count all customers
    @Query(value = "SELECT COUNT(*) FROM customers", nativeQuery = true)
    long countCustomersNative();

    // 7. Take money out, only if the balance is enough. Returns 1 if done, 0 if the balance is too low
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Customer c SET c.balance = c.balance - :amount " +
           "WHERE c.accountNumber = :accountNumber AND c.balance >= :amount")
    int debit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    // 8. Add money to an account
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Customer c SET c.balance = c.balance + :amount WHERE c.accountNumber = :accountNumber")
    int credit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
