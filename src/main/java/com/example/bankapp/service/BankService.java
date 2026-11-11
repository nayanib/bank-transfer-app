package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)          // default for every method: read-only transaction
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // ---------- Reading data (read-only) ----------

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    // All customers, or only the ones whose name matches the keyword
    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllOrdered();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public long countCustomers() {
        return customerRepository.countCustomersNative();
    }

    // SUM returns null when there are no customers, so return 0 instead
    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public long countByStatus(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    // ---------- Saving data (read-write) ----------

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number " + customer.getAccountNumber() + " already exists");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email " + customer.getEmail() + " already exists");
        }
        return customerRepository.save(customer);
    }

    /*
     * Moves money from one account to another.
     * Everything in this method is ONE transaction:
     *  - if the method finishes normally, Spring COMMITS (all changes are saved)
     *  - if any exception is thrown, Spring ROLLS BACK (all changes are undone)
     * rollbackFor = Exception.class: roll back for every exception, not only RuntimeException.
     * Do not catch the exception here, otherwise Spring never sees it and commits.
     */
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount,
                                    BigDecimal amount, boolean simulateFailure) {

        // Step 1: check that the accounts are different and both exist
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        if (customerRepository.countAccounts(fromAccount) == 0) {
            throw new AccountNotFoundException(fromAccount);
        }
        if (customerRepository.countAccounts(toAccount) == 0) {
            throw new AccountNotFoundException(toAccount);
        }

        // Step 2: take the money from the sender (0 rows updated = balance too low)
        int updated = customerRepository.debit(fromAccount, amount);
        if (updated == 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        // Step 3: experiment - fail after the withdrawal to test the rollback
        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        // Step 4: give the money to the receiver
        customerRepository.credit(toAccount, amount);

        // Step 5: save a SUCCESS record
        BankTransaction record = new BankTransaction(fromAccount, toAccount, amount,
                TransactionStatus.SUCCESS, "Transfer completed");
        return transactionRepository.save(record);
    }
}
