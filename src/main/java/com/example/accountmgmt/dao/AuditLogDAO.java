package com.example.accountmgmt.dao;

import com.example.accountmgmt.hibernate.model.AuditLog;

import java.util.Date;
import java.util.List;

public interface AuditLogDAO {

    void addAuditLog(AuditLog log);

    /**
     * 依條件查詢稽核紀錄（任一參數為 null 表示不套用該條件），依時間新到舊排序。
     *
     * @param actor            操作者（等值比對）
     * @param action           動作類型（等值比對）
     * @param targetAccountNo  目標帳號（等值比對）
     * @param success          成功與否（null=不限）
     * @param from             起始時間（含）
     * @param to               結束時間（含）
     */
    List<AuditLog> query(String actor, String action, String targetAccountNo,
                         Boolean success, String errorCode, Date from, Date to);
}
