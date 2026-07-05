package com.example.accountmgmt.hibernate.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * 稽核紀錄 entity（#9）。
 *
 * <p>記錄「誰（actor）在何時對哪個帳戶做了什麼動作、成功或失敗、失敗的 error code」。
 * 與 transactions 表的業務紀錄分離：transactions 記金流/狀態的業務事實，
 * audit_log 記跨切面的操作稽核（含操作者身分與失敗事件），供 admin 查詢除錯。</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 執行操作的登入者（admin 或客戶 username；系統動作可為 SYSTEM）。 */
    @Column(name = "actor", nullable = false, length = 50)
    private String actor;

    /** 動作類型：OPEN_ACCOUNT / DEPOSIT / WITHDRAW / TRANSFER / FREEZE / ACTIVATE / CLOSE / UPDATE_OWNER / CHANGE_PASSWORD 等。 */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    /** 操作對象帳號（部分動作可能為 null，如 CHANGE_PASSWORD）。 */
    @Column(name = "target_account_no", length = 20)
    private String targetAccountNo;

    /** 是否成功。 */
    @Column(name = "success", nullable = false)
    private boolean success = true;

    /** 失敗時的錯誤碼（成功為 null）。 */
    @Column(name = "error_code", length = 10)
    private String errorCode;

    /** 人類可讀的細節說明。 */
    @Column(name = "detail", length = 500)
    private String detail;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    public AuditLog() {
    }

    public AuditLog(String actor, String action, String targetAccountNo,
                    boolean success, String errorCode, String detail, Date createdAt) {
        this.actor = actor;
        this.action = action;
        this.targetAccountNo = targetAccountNo;
        this.success = success;
        this.errorCode = errorCode;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetAccountNo() {
        return targetAccountNo;
    }

    public void setTargetAccountNo(String targetAccountNo) {
        this.targetAccountNo = targetAccountNo;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
