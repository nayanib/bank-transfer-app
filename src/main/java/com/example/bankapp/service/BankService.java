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
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)          // default for every method: read-only transaction
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // bank account that receives the 1% transfer fee
    public static final String BANK_ACCOUNT = "999999";
    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");

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

    public List<BankTransaction> getLargeTransfers(BigDecimal min) {
        return transactionRepository.findLargeTransfers(min, TransactionStatus.SUCCESS);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        return transactionRepository.getTotalSent(accountNumber, TransactionStatus.SUCCESS);
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

        // 1% fee, rounded to 2 decimal places
        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        // Step 2: take the money + fee from the sender (0 rows updated = balance too low)
        int updated = customerRepository.debit(fromAccount, amount.add(fee));
        if (updated == 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        // Step 3: experiment - fail after the withdrawal to test the rollback
        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        // Step 4: give the money to the receiver and the fee to the bank account
        customerRepository.credit(toAccount, amount);
        customerRepository.credit(BANK_ACCOUNT, fee);

        // Step 5: save SUCCESS records for the transfer and the fee
        transactionRepository.save(new BankTransaction(fromAccount, BANK_ACCOUNT, fee,
                TransactionStatus.SUCCESS, "Transfer fee (1%)"));
        BankTransaction record = new BankTransaction(fromAccount, toAccount, amount,
                TransactionStatus.SUCCESS, "Transfer completed");
        return transactionRepository.save(record);
    }

    // Deposit: add money to one account (uses credit)
    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        checkAmount(amount);
        if (customerRepository.countAccounts(accountNumber) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        customerRepository.credit(accountNumber, amount);
        transactionRepository.save(new BankTransaction(null, accountNumber, amount,
                TransactionStatus.SUCCESS, "Deposit"));
    }

    // Withdraw: take money from one account (uses debit)
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        checkAmount(amount);
        if (customerRepository.countAccounts(accountNumber) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException(accountNumber);
        }
        transactionRepository.save(new BankTransaction(accountNumber, null, amount,
                TransactionStatus.SUCCESS, "Withdrawal"));
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than 0");
        }
    }
}
