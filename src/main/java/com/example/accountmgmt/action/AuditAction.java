package com.example.accountmgmt.action;

import com.example.accountmgmt.hibernate.model.AuditLog;
import com.example.accountmgmt.service.AuditService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 稽核紀錄查詢頁（#9，admin 專屬，URL 由 SecurityConfig 限制）。
 *
 * <p>支援多條件過濾：操作者 / 動作 / 目標帳號 / 成功與否 / 日期區間，方便除錯。</p>
 */
public class AuditAction extends ActionSupport {

    private String actor;
    private String action;
    private String targetAccountNo;
    /** "true" / "false" / 空 = 不限。 */
    private String success;
    /** 錯誤碼過濾（item6，等值），如 E2001。 */
    private String errorCode;
    /** detail 內容查詢（item6）。 */
    private String detailQuery;
    /** detail 查詢模式："regex" = 正則；其他（含 text/空）= 純文字 contains。 */
    private String detailMode;
    /** yyyy-MM-dd。 */
    private String fromDate;
    private String toDate;

    /** 排序欄位（id / createdAt / actor / action / targetAccountNo / success / errorCode）；空 = 預設（時間新到舊）。 */
    private String sortBy;
    /** 排序方向：asc / desc。 */
    private String sortDir;

    private List<AuditLog> auditLogs;

    @Autowired
    private AuditService auditService;

    public String list() {
        Boolean succ = null;
        if ("true".equalsIgnoreCase(success)) {
            succ = Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(success)) {
            succ = Boolean.FALSE;
        }
        boolean regex = "regex".equalsIgnoreCase(detailMode);
        try {
            auditLogs = auditService.query(
                    emptyToNull(actor), emptyToNull(action), emptyToNull(targetAccountNo),
                    succ, emptyToNull(errorCode), parseFrom(fromDate), parseTo(toDate),
                    emptyToNull(detailQuery), regex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // 例如 regex 語法錯誤（[E7001]）→ 顯示錯誤、清空結果，不中斷頁面
            addActionError(ex.getMessage());
        }
        applySort();
        return SUCCESS;
    }

    /**
     * 依 sortBy / sortDir 排序（item：Audit Log 可依欄位排序）。
     * sortBy 為空時維持預設（DAO 回傳的時間新到舊）。
     */
    private void applySort() {
        if (auditLogs == null || auditLogs.isEmpty() || sortBy == null || sortBy.trim().isEmpty()) {
            return;
        }
        final String col = sortBy.trim();
        final boolean asc = !"desc".equalsIgnoreCase(sortDir);
        List<AuditLog> sorted = new ArrayList<AuditLog>(auditLogs);
        Collections.sort(sorted, new Comparator<AuditLog>() {
            public int compare(AuditLog a, AuditLog b) {
                int c = cmp(a, b, col);
                return asc ? c : -c;
            }
        });
        auditLogs = sorted;
    }

    private int cmp(AuditLog a, AuditLog b, String col) {
        if ("id".equals(col)) {
            return nullSafe(a.getId(), b.getId());
        } else if ("actor".equals(col)) {
            return nullSafeStr(a.getActor(), b.getActor());
        } else if ("action".equals(col)) {
            return nullSafeStr(a.getAction(), b.getAction());
        } else if ("targetAccountNo".equals(col)) {
            return nullSafeStr(a.getTargetAccountNo(), b.getTargetAccountNo());
        } else if ("success".equals(col)) {
            return Boolean.valueOf(a.isSuccess()).compareTo(Boolean.valueOf(b.isSuccess()));
        } else if ("errorCode".equals(col)) {
            return nullSafeStr(a.getErrorCode(), b.getErrorCode());
        } else {
            // 預設 / createdAt
            return nullSafe(a.getCreatedAt(), b.getCreatedAt());
        }
    }

    private <T extends Comparable<T>> int nullSafe(T a, T b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private int nullSafeStr(String a, String b) {
        String x = (a == null) ? "" : a;
        String y = (b == null) ? "" : b;
        return x.compareToIgnoreCase(y);
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** 起始日 00:00:00。解析失敗回 null（略過此條件）。 */
    private Date parseFrom(String s) {
        Date d = parseDate(s);
        return d;
    }

    /** 結束日設為當日 23:59:59（含當日）。解析失敗回 null。 */
    private Date parseTo(String s) {
        Date d = parseDate(s);
        if (d == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return c.getTime();
    }

    private Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(s.trim());
        } catch (ParseException ex) {
            return null;
        }
    }

    // Getters and setters

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

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getDetailQuery() {
        return detailQuery;
    }

    public void setDetailQuery(String detailQuery) {
        this.detailQuery = detailQuery;
    }

    public String getDetailMode() {
        return detailMode;
    }

    public void setDetailMode(String detailMode) {
        this.detailMode = detailMode;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }
}
