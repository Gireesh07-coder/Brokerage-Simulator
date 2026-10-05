package com.gireesh.brokerage.controller;

import com.gireesh.brokerage.entity.Transaction;
import com.gireesh.brokerage.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> getTransactions(
            @PathVariable Long accountId) {

        return transactionService.getTransactions(accountId);
    }
}