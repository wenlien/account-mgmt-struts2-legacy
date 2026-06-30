package com.example.accountmgmt.dao;

import com.example.accountmgmt.hibernate.model.Transaction;

import java.util.List;

public interface TransactionDAO {
    List<Transaction> getTransactionsByAccount(String accountNo);
    void addTransaction(Transaction transaction);
}
