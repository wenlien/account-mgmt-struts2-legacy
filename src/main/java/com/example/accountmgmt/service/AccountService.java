package com.example.accountmgmt.service;

import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountCategory;
import com.example.accountmgmt.hibernate.model.AccountStatus;

import java.util.List;

public interface AccountService {

    List<Account> getAllAccounts();

    Account getAccountByNo(String accountNo);

    /**
     * 開立帳戶：依指定類別自動產生帳號（查 DB 最大序號 +1）。
     * 一律以 ACTIVE 開戶，openedDate 設為今日。
     *
     * @param ownerName 戶名
     * @param category  帳戶類別（TWD=A前綴 / FOREIGN=B前綴）
     * @return 新開立的 Account（含自動產生的帳號）
     */
    Account openAccount(String ownerName, String phone, String address, AccountCategory category, String ownerUsername);

    /**
     * 批次開戶：同一戶名可一次開立多個類別的帳戶，並標記擁有者登入帳號。
     *
     * @param ownerName     戶名
     * @param categories    帳戶類別（可多選）
     * @param ownerUsername 擁有者登入帳號（#7 用於過濾）
     * @return 新開立的所有 Account
     */
    List<Account> openAccounts(String ownerName, String phone, String address,
                               List<AccountCategory> categories, String ownerUsername);

    /**
     * 開戶並同時建立登入帳號（#6）：先建 user（BCrypt 雜湊），再開立帳戶並 stamp ownerUsername。
     * 整筆在同一交易內（原子性）。
     *
     * @param username    新登入帳號
     * @param rawPassword 明文密碼（僅雜湊用）
     * @param ownerName   戶名
     * @param categories  帳戶類別（可多選）
     * @return 新開立的所有 Account
     */
    List<Account> openAccountsForNewUser(String username, String rawPassword, String ownerName,
                                         String phone, String address, List<AccountCategory> categories);

    /** 取得指定登入者名下的所有帳戶（#7：user 只看自己）。 */
    List<Account> getAccountsByOwner(String ownerUsername);

    /**
     * 更新指定登入者名下所有帳戶的電話 / 地址（#6：電話為 identity，同步全帳戶）。
     * phone / address 必填。
     */
    void updateContactForOwner(String ownerUsername, String phone, String address);

    /** @deprecated 保留給舊測試用，新代碼請用 openAccount。 */
    @Deprecated
    void addAccount(Account account);

    /** 更新帳戶：僅可改戶名；openedDate、status、balance 不可經此變更；CLOSED 帳戶不可修改。 */
    void updateAccount(Account account);

    /**
     * 變更帳戶狀態（ACTIVE ↔ FROZEN）。CLOSED 為終態不可再變更；
     * 不可經此設為 CLOSED（請用 closeAccount）。狀態變更會寫入交易 log。
     *
     * @param note 狀態變更註記，必填：凍結為 freeze note（否則 {@code [E3004] FREEZE_NOTE_REQUIRED}）、
     *             解凍為 activate note（否則 {@code [E3005] ACTIVATE_NOTE_REQUIRED}）。
     */
    void changeStatus(String accountNo, AccountStatus newStatus, String note);

    /**
     * 關閉帳戶：餘額須為 0、需填註記、且帳戶尚未關閉；
     * 關閉後設 status=CLOSED、寫入 closedDate 與 closeNote，並記一筆 STATUS 交易。
     */
    void closeAccount(String accountNo, String note);

    /**
     * 批次關戶：逐一檢查餘額=0 後關閉。部分帳戶無法關閉時不影響其他帳戶，
     * 回傳失敗帳號與原因的 list。
     *
     * @param accountNos 要關閉的帳號清單
     * @param note       統一關戶註記
     * @return 失敗清單（帳號→原因）；空 list 表示全部成功
     */
    List<CloseAccountResult> closeAccounts(List<String> accountNos, String note);

    /**
     * 批次變更狀態（item7：批次 freeze / activate）。逐一呼叫 changeStatus，
     * 部分失敗不影響其他帳戶，回傳每個帳號的成功/失敗（+原因）。
     * newStatus 不可為 CLOSED（關戶請用 closeAccounts）。
     * note 必填（凍結與解凍皆是，套用到每一筆）。
     */
    List<CloseAccountResult> changeStatusBatch(List<String> accountNos, AccountStatus newStatus, String note);
}
