package com.example.accountmgmt.service;

/**
 * 餘額不足時拋出（對應 design.md §5 contract 的 HTTP 409）。
 *
 * <p>#5：帶錯誤碼 {@link BankErrorCode#INSUFFICIENT_BALANCE}（E2001），
 * 訊息前綴 {@code [E2001]}。保留 extends {@link RuntimeException} 與既有型別，
 * 既有 {@code @Test(expected = InsufficientBalanceException.class)} 不受影響。</p>
 */
public class InsufficientBalanceException extends RuntimeException implements CodedException {

    private final BankErrorCode errorCode = BankErrorCode.INSUFFICIENT_BALANCE;

    public InsufficientBalanceException(String message) {
        super(CodedException.format(BankErrorCode.INSUFFICIENT_BALANCE, message));
    }

    @Override
    public BankErrorCode getErrorCode() {
        return errorCode;
    }
}
