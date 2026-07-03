package com.example.accountmgmt.service;

import com.example.accountmgmt.hibernate.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    /** 存款：增加餘額並記一筆 DEPOSIT 交易。 */
    void deposit(String accountNo, BigDecimal amount);

    /** 提款：餘額足夠才扣款並記一筆 WITHDRAW 交易，否則拋 InsufficientBalanceException。 */
    void withdraw(String accountNo, BigDecimal amount);

    /**
     * 轉帳：從 fromAccountNo 轉 amount 到 toAccountNo。
     * 來源與目標帳戶皆須存在且狀態為 ACTIVE，來源餘額須足夠，且不可轉給自己。
     * 內部以「來源 WITHDRAW + 目標 DEPOSIT」兩筆交易記錄，整筆原子性（@Transactional）。
     */
    void transfer(String fromAccountNo, String toAccountNo, BigDecimal amount);

    List<Transaction> getTransactions(String accountNo);
}
