package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    // Dashboard: statistics, forms, customers table, latest transactions
    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        loadDashboard(model, keyword);
        return "bank";
    }

    // Add a customer
    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult result, Model model,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) {
            // show the form again with the error messages
            loadDashboard(model, null);
            return "bank";
        }
        try {
            bankService.createCustomer(customer);
            redirect.addFlashAttribute("success", "Customer " + customer.getFullName() + " added");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    // Transfer money
    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult result, Model model,
                           RedirectAttributes redirect) {
        if (result.hasErrors()) {
            // form is not valid: nothing is saved
            loadDashboard(model, null);
            return "bank";
        }
        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            redirect.addFlashAttribute("success", "Transfer of LKR " + request.getAmount() + " completed");
        } catch (Exception e) {
            // This runs AFTER the transfer was rolled back.
            // Save a FAILED record in a new transaction so it is not lost.
            auditService.recordFailure(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), e.getMessage());
            redirect.addFlashAttribute("error",
                    "Transfer failed: " + e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }

    // Transactions of one account
    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model) {
        model.addAttribute("customer", bankService.getCustomer(accountNumber));
        model.addAttribute("transactions", bankService.getHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
        return "history";
    }

    // Successful transfers above a minimum amount, e.g. /large-transfers?min=10000
    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    // Deposit form
    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                          RedirectAttributes redirect) {
        try {
            bankService.deposit(accountNumber, amount);
            redirect.addFlashAttribute("success", "Deposited LKR " + amount + " to account " + accountNumber);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Deposit failed: " + e.getMessage());
        }
        return "redirect:/deposit";
    }

    // Withdraw form
    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                           RedirectAttributes redirect) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirect.addFlashAttribute("success", "Withdrew LKR " + amount + " from account " + accountNumber);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Withdrawal failed: " + e.getMessage());
        }
        return "redirect:/withdraw";
    }

    // Puts everything the dashboard page needs into the model
    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("customers", bankService.searchCustomers(keyword));
        model.addAttribute("allCustomers", bankService.getAllCustomers());
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("totalCustomers", bankService.countCustomers());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countByStatus(TransactionStatus.FAILED));
        // empty forms (keep the old ones if they have errors)
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
    }
}
