package com.example.accountmgmt.hibernate.model;

/**
 * 交易類型。
 * 對應 requirements.md D1：只做 DEPOSIT / WITHDRAW，不含轉帳。
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW
}
