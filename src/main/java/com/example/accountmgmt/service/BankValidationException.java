package com.example.accountmgmt.service;

/**
 * 輸入 / 參數驗證類錯誤（帶錯誤碼）。
 *
 * <p>刻意 extends {@link IllegalArgumentException}，讓既有以
 * {@code @Test(expected = IllegalArgumentException.class)} 撰寫的測試維持有效
 * （IS-A），同時透過 {@link CodedException} 攜帶 {@link BankErrorCode}。
 * 訊息統一組成 {@code [Exxxx] message}。</p>
 */
public class BankValidationException extends IllegalArgumentException implements CodedException {

    private final BankErrorCode errorCode;

    public BankValidationException(BankErrorCode errorCode, String message) {
        super(CodedException.format(errorCode, message));
        this.errorCode = errorCode;
    }

    @Override
    public BankErrorCode getErrorCode() {
        return errorCode;
    }
}
