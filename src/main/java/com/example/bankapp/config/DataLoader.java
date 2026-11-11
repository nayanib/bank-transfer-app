package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Inserts three sample customers the first time the application starts (empty database). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        // only when the table is empty, so we don't add them again on every restart
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    new Customer("Kamal Silva", "kamal@example.com", "100001", new BigDecimal("50000.00")),
                    new Customer("Nimali Perera", "nimali@example.com", "100002", new BigDecimal("25000.00")),
                    new Customer("Saman Kumara", "saman@example.com", "100003", new BigDecimal("10000.00"))
            ));
            // total money = LKR 85,000.00
        }
    }
}
