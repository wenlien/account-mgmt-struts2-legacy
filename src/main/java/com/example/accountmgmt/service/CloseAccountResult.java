package com.example.accountmgmt.service;

/**
 * 批次關戶的單筆結果（成功或失敗）。
 */
public class CloseAccountResult {

    private final String accountNo;
    private final boolean success;
    private final String reason;

    public CloseAccountResult(String accountNo, boolean success, String reason) {
        this.accountNo = accountNo;
        this.success = success;
        this.reason = reason;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }
}
