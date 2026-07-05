package com.example.accountmgmt.hibernate.model;

/**
 * 帳戶狀態。
 * 對應 design.md §3.1：status enum (ACTIVE/FROZEN/CLOSED)。
 */
public enum AccountStatus {
    ACTIVATED,
    FROZEN,
    CLOSED
}
