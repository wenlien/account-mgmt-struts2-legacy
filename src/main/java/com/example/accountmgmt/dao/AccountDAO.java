package com.example.accountmgmt.dao;

import com.example.accountmgmt.hibernate.model.Account;

import java.util.List;

public interface AccountDAO {
    List<Account> getAllAccounts();
    Account getAccountByNo(String accountNo);

    /** 取得指定登入者名下的所有帳戶（#7：user 只看自己）。 */
    List<Account> getAccountsByOwnerUsername(String ownerUsername);

    /** 取得指定電話的所有帳戶（#5：電話為 identity，用於每電話每類型限一戶檢查）。 */
    List<Account> getAccountsByPhone(String phone);

    void addAccount(Account account);
    void updateAccount(Account account);

    /**
     * 取得指定前綴（如 'A' 或 'B'）的最大帳號數字部分。
     * 例如前綴 'A' 若有 A001~A004 則回傳 4；無帳號時回傳 0。
     */
    int getMaxSequence(char prefix);
}
