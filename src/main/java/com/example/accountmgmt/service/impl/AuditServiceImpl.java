package com.example.accountmgmt.service.impl;

import com.example.accountmgmt.dao.AuditLogDAO;
import com.example.accountmgmt.hibernate.model.AuditLog;
import com.example.accountmgmt.service.AuditService;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditLogDAO auditLogDAO;

    public void setAuditLogDAO(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    @Transactional
    public void logSuccess(String actor, String action, String targetAccountNo, String detail) {
        auditLogDAO.addAuditLog(new AuditLog(safe(actor), action, targetAccountNo,
                true, null, detail, new Date()));
    }

    @Override
    @Transactional
    public void logFailure(String actor, String action, String targetAccountNo, String errorCode, String detail) {
        auditLogDAO.addAuditLog(new AuditLog(safe(actor), action, targetAccountNo,
                false, errorCode, detail, new Date()));
    }

    @Override
    @Transactional
    public List<AuditLog> query(String actor, String action, String targetAccountNo,
                                Boolean success, String errorCode, Date from, Date to,
                                String detailQuery, boolean detailRegex) {
        // 結構化條件（含 errorCode）交給 DB；detail 的文字/regex 查詢在 Java 端做
        // （HQL 無 REGEXP，且 audit_log 屬小量資料，記憶體過濾成本可接受）。
        List<AuditLog> rows = auditLogDAO.query(actor, action, targetAccountNo, success, errorCode, from, to);
        if (detailQuery == null || detailQuery.trim().isEmpty()) {
            return rows;
        }
        String q = detailQuery.trim();
        List<AuditLog> filtered = new ArrayList<AuditLog>();
        if (detailRegex) {
            Pattern p;
            try {
                p = Pattern.compile(q);
            } catch (PatternSyntaxException ex) {
                throw new BankValidationException(BankErrorCode.INVALID_REGEX,
                        "Invalid regex: " + ex.getDescription());
            }
            for (AuditLog r : rows) {
                if (r.getDetail() != null && p.matcher(r.getDetail()).find()) {
                    filtered.add(r);
                }
            }
        } else {
            String needle = q.toLowerCase();
            for (AuditLog r : rows) {
                if (r.getDetail() != null && r.getDetail().toLowerCase().contains(needle)) {
                    filtered.add(r);
                }
            }
        }
        return filtered;
    }

    @Override
    @Transactional
    public List<AuditLog> profileChangeHistory(String username) {
        List<AuditLog> result = new ArrayList<AuditLog>();
        if (username == null || username.trim().isEmpty()) {
            return result;
        }
        String needle = "user=" + username.trim();
        // owner name / phone / address 變更（UPDATE_OWNER）+ 密碼變更（CHANGE_PASSWORD），
        // 皆以 detail 內含 "user=<username>" 過濾出屬於該使用者的紀錄。
        result.addAll(query(null, "UPDATE_OWNER", null, null, null, null, null, needle, false));
        result.addAll(query(null, "CHANGE_PASSWORD", null, null, null, null, null, needle, false));
        // 時間新到舊。
        Collections.sort(result, new Comparator<AuditLog>() {
            public int compare(AuditLog a, AuditLog b) {
                Date da = a.getCreatedAt();
                Date db = b.getCreatedAt();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            }
        });
        return result;
    }

    /** actor 不可為空（未登入的系統動作記為 SYSTEM）。 */
    private String safe(String actor) {
        return (actor == null || actor.trim().isEmpty()) ? "SYSTEM" : actor.trim();
    }
}
