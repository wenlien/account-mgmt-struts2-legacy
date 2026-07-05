package com.example.accountmgmt.service;

/**
 * 狀態不合法類錯誤（帶錯誤碼）。
 *
 * <p>刻意 extends {@link IllegalStateException}，讓既有以
 * {@code @Test(expected = IllegalStateException.class)} 撰寫的測試維持有效
 * （IS-A），同時透過 {@link CodedException} 攜帶 {@link BankErrorCode}。
 * 訊息統一組成 {@code [Exxxx] message}。</p>
 */
public class BankStateException extends IllegalStateException implements CodedException {

    private final BankErrorCode errorCode;

    public BankStateException(BankErrorCode errorCode, String message) {
        super(CodedException.format(errorCode, message));
        this.errorCode = errorCode;
    }

    @Override
    public BankErrorCode getErrorCode() {
        return errorCode;
    }
}
