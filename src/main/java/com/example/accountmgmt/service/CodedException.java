package com.example.accountmgmt.service;

/**
 * 帶錯誤碼的例外共同介面（#5）。
 *
 * <p>Java 例外無法多重繼承，故用介面讓不同基底類別（
 * {@link IllegalArgumentException} / {@link IllegalStateException} /
 * {@link RuntimeException}）的例外都能攜帶 {@link BankErrorCode}，
 * 並在 catch 層統一取出 error code。</p>
 */
public interface CodedException {

    BankErrorCode getErrorCode();

    /** 組裝成 {@code [Exxxx] message} 的輔助方法。 */
    static String format(BankErrorCode code, String message) {
        return "[" + code.getCode() + "] " + message;
    }

    /** 從任意例外取出錯誤碼字串；非 CodedException 回 null（供稽核記錄失敗用）。 */
    static String codeOf(Throwable t) {
        return (t instanceof CodedException) ? ((CodedException) t).getErrorCode().getCode() : null;
    }
}
