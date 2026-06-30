package com.example.accountmgmt.action;

import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.service.InsufficientBalanceException;
import com.example.accountmgmt.service.TransactionService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

/**
 * 存款 / 提款 / 查交易紀錄的 Struts2 Action。
 */
public class TransactionAction extends ActionSupport {

    private String accountNo;
    private BigDecimal amount;
    private List<Transaction> transactionList;

    @Autowired
    private TransactionService transactionService;

    public String list() {
        transactionList = transactionService.getTransactions(accountNo);
        return SUCCESS;
    }

    public String deposit() {
        try {
            transactionService.deposit(accountNo, amount);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            addActionError(ex.getMessage());
            return INPUT;
        }
        return SUCCESS;
    }

    public String withdraw() {
        try {
            transactionService.withdraw(accountNo, amount);
        } catch (InsufficientBalanceException ex) {
            addActionError(ex.getMessage());
            return INPUT;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            addActionError(ex.getMessage());
            return INPUT;
        }
        return SUCCESS;
    }

    // Getters and setters

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }
}
