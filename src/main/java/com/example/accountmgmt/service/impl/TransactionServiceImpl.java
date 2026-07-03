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
    public void transfer(String fromAccountNo, String toAccountNo, BigDecimal amount) {
        requirePositiveAmount(amount);
        if (fromAccountNo == null || fromAccountNo.equals(toAccountNo)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        // 來源與目標都必須存在且為 ACTIVE
        Account from = loadActiveAccount(fromAccountNo);
        Account to = loadActiveAccount(toAccountNo);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for account " + fromAccountNo);
        }
        Date now = new Date();
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountDAO.updateAccount(from);
        accountDAO.updateAccount(to);
        // 以既有交易類型記兩筆：來源 WITHDRAW、目標 DEPOSIT
        transactionDAO.addTransaction(new Transaction(from, TransactionType.WITHDRAW, amount, now));
        transactionDAO.addTransaction(new Transaction(to, TransactionType.DEPOSIT, amount, now));
    }

    @Override
    @Transactional
    public List<Transaction> getTransactions(String accountNo) {
        return transactionDAO.getTransactionsByAccount(accountNo);
    }

    /** 金額須 > 0。 */
    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    /** 帳戶須存在且狀態為 ACTIVE。 */
    private Account loadActiveAccount(String accountNo) {
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

    /** 共用前置驗證：金額 > 0、帳戶存在、狀態為 ACTIVE（deposit / withdraw 用）。 */
    private Account loadActiveAccount(String accountNo, BigDecimal amount) {
        requirePositiveAmount(amount);
        return loadActiveAccount(accountNo);
    }
}
