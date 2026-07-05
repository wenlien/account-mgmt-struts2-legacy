package com.example.accountmgmt.hibernate.model;

/**
 * 交易 / 帳務事件類型（也作為稽核 log 的事件類型）。
 * DEPOSIT / WITHDRAW：金額異動。轉帳以 WITHDRAW + DEPOSIT 兩筆表示。
 * STATUS：帳戶狀態變更事件（amount = 0，細節記於 Transaction.note）。
 * 注意：值長度須 <= 10（對應 transactions.type VARCHAR(10)）。
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    STATUS
}
