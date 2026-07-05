package com.example.accountmgmt.service;

import com.example.accountmgmt.dao.AuditLogDAO;
import com.example.accountmgmt.hibernate.model.AuditLog;
import com.example.accountmgmt.service.impl.AuditServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * AuditService 的 JUnit 4 測試，聚焦 item6 的 detail 文字/regex 過濾與壞 regex 錯誤。
 * fake DAO 回傳固定清單（結構化條件由真 DAO 的 HQL 負責，此處只驗 Java 端 detail 過濾）。
 */
public class AuditServiceTest {

    private AuditServiceImpl service;

    @Before
    public void setUp() {
        FakeAuditLogDAO dao = new FakeAuditLogDAO(Arrays.asList(
                row("-> A002 amount=150.00 note=rent"),
                row("amount=500.00"),
                row("-> B002 amount=300.00 note=fx")));
        service = new AuditServiceImpl();
        service.setAuditLogDAO(dao);
    }

    private AuditLog row(String detail) {
        return new AuditLog("alice", "TRANSFER", "A001", true, null, detail, new Date());
    }

    private List<AuditLog> query(String detailQuery, boolean regex) {
        return service.query(null, null, null, null, null, null, null, detailQuery, regex);
    }

    @Test
    public void noDetailQuery_returnsAll() {
        assertEquals(3, query(null, false).size());
    }

    @Test
    public void detailText_contains_matches() {
        assertEquals(1, query("rent", false).size());
    }

    @Test
    public void detailText_caseInsensitive() {
        assertEquals(1, query("RENT", false).size());
    }

    @Test
    public void detailText_noMatch_empty() {
        assertEquals(0, query("nonexistent", false).size());
    }

    @Test
    public void detailRegex_alternation_matches() {
        // fx|salary → 只有 fx 那筆命中
        assertEquals(1, query("fx|salary", true).size());
    }

    @Test
    public void detailRegex_matchesMultiple() {
        // note=(rent|fx) → 兩筆
        assertEquals(2, query("note=(rent|fx)", true).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void detailRegex_invalid_throws() {
        query("[", true); // 未閉合字元類 → BankValidationException(E7001)
    }

    // ---- 本輪：profileChangeHistory（Edit Owner 頁面就地變更歷史）----

    @Test
    public void profileChangeHistory_filtersByUserAndAction() {
        FakeAuditLogDAO dao = new FakeAuditLogDAO(Arrays.asList(
                new AuditLog("admin", "UPDATE_OWNER", null, true, null,
                        "user=alice name/phone/address updated", new Date(1000L)),
                new AuditLog("alice", "CHANGE_PASSWORD", null, true, null,
                        "user=alice self password changed", new Date(2000L)),
                new AuditLog("admin", "UPDATE_OWNER", null, true, null,
                        "user=bob name/phone/address updated", new Date(1500L)),
                new AuditLog("admin", "OPEN_ACCOUNT", "A001", true, null,
                        "user=alice opened account", new Date(500L))));
        AuditServiceImpl svc = new AuditServiceImpl();
        svc.setAuditLogDAO(dao);

        List<AuditLog> hist = svc.profileChangeHistory("alice");
        // 只含 alice 的 UPDATE_OWNER + CHANGE_PASSWORD（排除 bob、排除 OPEN_ACCOUNT）
        assertEquals(2, hist.size());
        // 新到舊：CHANGE_PASSWORD(2000) 在 UPDATE_OWNER(1000) 之前
        assertEquals("CHANGE_PASSWORD", hist.get(0).getAction());
        assertEquals("UPDATE_OWNER", hist.get(1).getAction());
    }

    @Test
    public void profileChangeHistory_blankUser_empty() {
        assertEquals(0, service.profileChangeHistory("  ").size());
    }

    // ---- in-memory fake ----

    static class FakeAuditLogDAO implements AuditLogDAO {
        private final List<AuditLog> rows;

        FakeAuditLogDAO(List<AuditLog> rows) {
            this.rows = new ArrayList<AuditLog>(rows);
        }

        public void addAuditLog(AuditLog log) {
            rows.add(log);
        }

        public List<AuditLog> query(String actor, String action, String targetAccountNo,
                                    Boolean success, String errorCode, Date from, Date to) {
            // 模擬 DB 的等值結構化過濾（null 表示不套用）；dates 忽略。
            List<AuditLog> out = new ArrayList<AuditLog>();
            for (AuditLog r : rows) {
                if (actor != null && !actor.equals(r.getActor())) continue;
                if (action != null && !action.equals(r.getAction())) continue;
                if (targetAccountNo != null && !targetAccountNo.equals(r.getTargetAccountNo())) continue;
                if (success != null && !success.equals(r.isSuccess())) continue;
                if (errorCode != null && !errorCode.equals(r.getErrorCode())) continue;
                out.add(r);
            }
            return out;
        }
    }
}
