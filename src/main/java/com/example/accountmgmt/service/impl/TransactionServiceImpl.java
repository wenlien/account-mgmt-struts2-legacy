package com.example.accountmgmt.service.impl;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.dao.TransactionDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.hibernate.model.TransactionType;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankStateException;
import com.example.accountmgmt.service.BankValidationException;
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
        Transaction tx = new Transaction(account, TransactionType.DEPOSIT, amount, new Date());
        tx.setBalanceAfter(account.getBalance());
        transactionDAO.addTransaction(tx);
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
        Transaction tx = new Transaction(account, TransactionType.WITHDRAW, amount, new Date());
        tx.setBalanceAfter(account.getBalance());
        transactionDAO.addTransaction(tx);
    }

    @Override
    @Transactional
    public void transfer(String fromAccountNo, String toAccountNo, BigDecimal amount, String note) {
        requirePositiveAmount(amount);
        if (fromAccountNo == null || fromAccountNo.equals(toAccountNo)) {
            throw new BankValidationException(BankErrorCode.SAME_ACCOUNT_TRANSFER,
                    "Cannot transfer to the same account");
        }
        // 同類別才可轉帳（A→A / B→B）
        if (fromAccountNo.charAt(0) != toAccountNo.charAt(0)) {
            throw new BankValidationException(BankErrorCode.CROSS_CATEGORY_TRANSFER,
                    "Cross-category transfer not allowed: " + fromAccountNo + " -> " + toAccountNo);
        }
        // 轉帳註記為選填，但填寫時最多 7 字元（server 端驗證，勿只靠前端 maxlength）
        String transferNote = normalizeNote(note);
        // 來源與目標都必須存在且為 ACTIVATED
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
        // 來源 WITHDRAW leg（含 targetAccountNo + balanceAfter + note）
        Transaction withdrawTx = new Transaction(from, TransactionType.WITHDRAW, amount, now);
        withdrawTx.setBalanceAfter(from.getBalance());
        withdrawTx.setTargetAccountNo(toAccountNo);
        withdrawTx.setNote(transferNote);
        transactionDAO.addTransaction(withdrawTx);
        // 目標 DEPOSIT leg（含 balanceAfter + 同一筆 note）
        Transaction depositTx = new Transaction(to, TransactionType.DEPOSIT, amount, now);
        depositTx.setBalanceAfter(to.getBalance());
        depositTx.setNote(transferNote);
        transactionDAO.addTransaction(depositTx);
    }

    /** 轉帳註記上限（字元數）。 */
    private static final int MAX_TRANSFER_NOTE_LENGTH = 7;

    /**
     * 正規化轉帳註記：空白視為無註記（回 null）；填寫時長度不得超過 {@value #MAX_TRANSFER_NOTE_LENGTH} 字元。
     */
    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_TRANSFER_NOTE_LENGTH) {
            throw new BankValidationException(BankErrorCode.TRANSFER_NOTE_TOO_LONG,
                    "Transfer note must be at most " + MAX_TRANSFER_NOTE_LENGTH + " characters");
        }
        return trimmed;
    }

    @Override
    @Transactional
    public List<Transaction> getTransactions(String accountNo) {
        return transactionDAO.getTransactionsByAccount(accountNo);
    }

    /** 金額須 > 0。 */
    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankValidationException(BankErrorCode.NON_POSITIVE_AMOUNT,
                    "Amount must be greater than zero");
        }
    }

    /** 帳戶須存在且狀態為 ACTIVATED。 */
    private Account loadActiveAccount(String accountNo) {
        Account account = accountDAO.getAccountByNo(accountNo);
        if (account == null) {
            throw new BankValidationException(BankErrorCode.ACCOUNT_NOT_FOUND,
                    "Account not found: " + accountNo);
        }
        if (account.getStatus() != AccountStatus.ACTIVATED) {
            throw new BankStateException(BankErrorCode.ACCOUNT_NOT_ACTIVE,
                    "Account is not active: " + accountNo + " (" + account.getStatus() + ")");
        }
        return account;
    }

    /** 共用前置驗證：金額 > 0、帳戶存在、狀態為 ACTIVATED（deposit / withdraw 用）。 */
    private Account loadActiveAccount(String accountNo, BigDecimal amount) {
        requirePositiveAmount(amount);
        return loadActiveAccount(accountNo);
    }
}
