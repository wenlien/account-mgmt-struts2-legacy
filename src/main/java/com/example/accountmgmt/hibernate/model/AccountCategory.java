package com.example.accountmgmt.hibernate.model;

/**
 * 帳戶類別（決定帳號前綴與轉帳限制）。
 *
 * <ul>
 *   <li>TWD（前綴 A）：台幣帳戶，帳號格式 A001~A999</li>
 *   <li>FOREIGN（前綴 B）：外幣帳戶，帳號格式 B001~B999</li>
 * </ul>
 *
 * 規則：只允許同類別帳號之間轉帳。
 */
public enum AccountCategory {

    /** 台幣帳戶（帳號前綴 A）。 */
    TWD('A'),

    /** 外幣帳戶（帳號前綴 B）。 */
    FOREIGN('B');

    private final char prefix;

    AccountCategory(char prefix) {
        this.prefix = prefix;
    }

    public char getPrefix() {
        return prefix;
    }

    /** 依帳號前綴字元反查類別；找不到拋 IllegalArgumentException。 */
    public static AccountCategory fromPrefix(char prefix) {
        for (AccountCategory c : values()) {
            if (c.prefix == prefix) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown account prefix: " + prefix);
    }

    /** 依帳號字串（如 "A001"）反查類別。 */
    public static AccountCategory fromAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isEmpty()) {
            throw new IllegalArgumentException("accountNo is required");
        }
        return fromPrefix(accountNo.charAt(0));
    }
}
