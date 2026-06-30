package com.example.accountmgmt.service;

import com.example.accountmgmt.hibernate.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    /** 存款：增加餘額並記一筆 DEPOSIT 交易。 */
    void deposit(String accountNo, BigDecimal amount);

    /** 提款：餘額足夠才扣款並記一筆 WITHDRAW 交易，否則拋 InsufficientBalanceException。 */
    void withdraw(String accountNo, BigDecimal amount);

    List<Transaction> getTransactions(String accountNo);
}
