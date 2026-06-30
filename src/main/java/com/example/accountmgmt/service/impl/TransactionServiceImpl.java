package com.example.accountmgmt.service.impl;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.dao.TransactionDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.hibernate.model.TransactionType;
import com.example.accountmgmt.service.InsufficientBalanceException;
import com.example.accountmgmt.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private TransactionDAO transactionDAO;

    /** 供測試 / XML setter 注入用。 */
    public void setAccountDAO(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    public void setTransactionDAO(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    @Override
    @Transactional
    public void deposit(String accountNo, BigDecimal amount) {
        Account account = loadActiveAccount(accountNo, amount);
        account.setBalance(account.getBalance().add(amount));
        accountDAO.updateAccount(account);
        transactionDAO.addTransaction(
                new Transaction(account, TransactionType.DEPOSIT, amount, new Date()));
    }

    @Override
    @Transactional
    public void withdraw(String accountNo, BigDecimal amount) {
        Account account = loadActiveAccount(accountNo, amount);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for account " + accountNo);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountDAO.updateAccount(account);
        transactionDAO.addTransaction(
                new Transaction(account, TransactionType.WITHDRAW, amount, new Date()));
    }

    @Override
    @Transactional
    public List<Transaction> getTransactions(String accountNo) {
        return transactionDAO.getTransactionsByAccount(accountNo);
    }

    /** 共用前置驗證：金額 > 0、帳戶存在、狀態為 ACTIVE。 */
    private Account loadActiveAccount(String accountNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        Account account = accountDAO.getAccountByNo(accountNo);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNo);
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Account is not active: " + accountNo + " (" + account.getStatus() + ")");
        }
        return account;
    }
}
