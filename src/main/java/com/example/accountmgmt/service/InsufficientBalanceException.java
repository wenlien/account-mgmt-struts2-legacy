package com.example.accountmgmt.service;

/**
 * 餘額不足時拋出（對應 design.md §5 contract 的 HTTP 409）。
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
