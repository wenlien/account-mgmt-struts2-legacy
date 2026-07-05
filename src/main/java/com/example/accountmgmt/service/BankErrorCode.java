package com.example.accountmgmt.service;

/**
 * 銀行系統統一錯誤碼（#5）。
 *
 * <p>格式 {@code Exxxx}：第一碼分類（1=帳戶、2=交易、3=生命週期、4=開戶輸入、
 * 5=使用者/認證、6=授權）。所有對使用者顯示的錯誤訊息一律前綴 {@code [Exxxx]}，
 * 由 {@link CodedException#getMessage()} 統一組裝，方便除錯與比對。</p>
 */
public enum BankErrorCode {

    // ---- 1xxx 帳戶 ----
    ACCOUNT_NOT_FOUND("E1001"),
    ACCOUNT_NOT_ACTIVE("E1002"),
    ACCOUNT_CLOSED("E1003"),
    ACCOUNT_NUMBER_OVERFLOW("E1004"),

    // ---- 2xxx 交易 ----
    INSUFFICIENT_BALANCE("E2001"),
    NON_POSITIVE_AMOUNT("E2002"),
    SAME_ACCOUNT_TRANSFER("E2003"),
    CROSS_CATEGORY_TRANSFER("E2004"),
    TRANSFER_NOTE_TOO_LONG("E2005"),

    // ---- 3xxx 帳戶生命週期 ----
    CLOSE_NOTE_REQUIRED("E3001"),
    CLOSE_BALANCE_NOT_ZERO("E3002"),
    INVALID_STATUS_CHANGE("E3003"),
    FREEZE_NOTE_REQUIRED("E3004"),
    ACTIVATE_NOTE_REQUIRED("E3005"),

    // ---- 4xxx 開戶輸入 ----
    OWNER_NAME_REQUIRED("E4001"),
    CATEGORY_REQUIRED("E4002"),
    PHONE_REQUIRED("E4003"),
    ADDRESS_REQUIRED("E4004"),
    DUPLICATE_ACCOUNT_FOR_PHONE("E4005"),

    // ---- 5xxx 使用者 / 認證 ----
    USERNAME_REQUIRED("E5001"),
    USERNAME_ALREADY_EXISTS("E5002"),
    PASSWORD_TOO_SHORT("E5003"),
    USER_NOT_FOUND("E5004"),
    OLD_PASSWORD_MISMATCH("E5005"),
    PASSWORD_MISMATCH("E5006"),

    // ---- 6xxx 授權 ----
    FORBIDDEN("E6001"),
    NOT_ACCOUNT_OWNER("E6002"),
    ADMIN_CANNOT_TRANSACT("E6003"),

    // ---- 7xxx 查詢 / 輸入 ----
    INVALID_REGEX("E7001");

    private final String code;

    BankErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
