package com.example.accountmgmt.service;

import com.example.accountmgmt.hibernate.model.AuditLog;

import java.util.Date;
import java.util.List;

/**
 * 稽核服務（#9）：記錄所有交易 / 狀態變更等操作，並提供 admin 過濾查詢。
 */
public interface AuditService {

    /** 記一筆成功的稽核。 */
    void logSuccess(String actor, String action, String targetAccountNo, String detail);

    /** 記一筆失敗的稽核（帶 error code）。 */
    void logFailure(String actor, String action, String targetAccountNo, String errorCode, String detail);

    /**
     * 依條件查詢（任一為 null 表示不套用），時間新到舊。
     *
     * @param errorCode   錯誤碼過濾（item6，等值）
     * @param detailQuery detail 內容查詢字串（item6）
     * @param detailRegex true=把 detailQuery 當 regex；false=純文字 contains（不分大小寫）
     */
    List<AuditLog> query(String actor, String action, String targetAccountNo,
                         Boolean success, String errorCode, Date from, Date to,
                         String detailQuery, boolean detailRegex);

    /**
     * 指定使用者的「profile 變更歷史」：owner name / 密碼 / 電話 / 地址 的變更紀錄
     * （動作 UPDATE_OWNER 與 CHANGE_PASSWORD，detail 內含該 username），時間新到舊。
     * 供 Edit Owner 頁面就地呈現該使用者的變更 log。
     */
    List<AuditLog> profileChangeHistory(String username);
}
