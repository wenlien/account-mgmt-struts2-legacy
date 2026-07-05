package com.example.accountmgmt.service;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.dao.TransactionDAO;
import com.example.accountmgmt.dao.UserDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountCategory;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.hibernate.model.TransactionType;
import com.example.accountmgmt.hibernate.model.User;
import com.example.accountmgmt.service.impl.AccountServiceImpl;
import com.example.accountmgmt.service.impl.UserServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * AccountService 的 JUnit 4 測試（含 @Before）。
 * 涵蓋 CRUD、帳戶生命週期（凍結/解凍/關戶）與狀態變更寫入交易 log。
 * 用 in-memory fake DAO，不依賴 Spring / DB。
 */
public class AccountServiceTest {

    private AccountServiceImpl service;
    private FakeAccountDAO accountDAO;
    private FakeTransactionDAO transactionDAO;
    private FakeUserDAO userDAO;
    private UserServiceImpl userService;

    @Before
    public void setUp() {
        accountDAO = new FakeAccountDAO();
        transactionDAO = new FakeTransactionDAO();
        userDAO = new FakeUserDAO();
        userService = new UserServiceImpl();
        userService.setUserDAO(userDAO);
        userService.setPasswordEncoder(new BCryptPasswordEncoder());
        service = new AccountServiceImpl();
        service.setAccountDAO(accountDAO);
        service.setTransactionDAO(transactionDAO);
        service.setUserService(userService);
    }

    private Account active(String no, BigDecimal balance) {
        return new Account(no, "Owner-" + no, balance, new Date(), AccountStatus.ACTIVATED);
    }

    // ---- openAccount（自動累進帳號）----

    @Test
    public void openAccount_TWD_generatesA001() {
        Account a = service.openAccount("Test User", null, null, AccountCategory.TWD, "u1");
        assertEquals("A001", a.getAccountNo());
        assertEquals(AccountStatus.ACTIVATED, a.getStatus());
        assertEquals(BigDecimal.ZERO, a.getBalance());
    }

    @Test
    public void openAccount_TWD_incrementsSequence() {
        service.openAccount("U1", null, null, AccountCategory.TWD, "u1");
        Account a2 = service.openAccount("U2", null, null, AccountCategory.TWD, "u2");
        assertEquals("A002", a2.getAccountNo());
    }

    @Test
    public void openAccount_FOREIGN_generatesB001() {
        Account b = service.openAccount("Test User", null, null, AccountCategory.FOREIGN, "u1");
        assertEquals("B001", b.getAccountNo());
    }

    @Test
    public void openAccounts_multiCategory_createsBoth() {
        List<Account> opened = service.openAccounts("Multi", null, null,
                Arrays.asList(AccountCategory.TWD, AccountCategory.FOREIGN), "multi");
        assertEquals(2, opened.size());
        assertEquals("A001", opened.get(0).getAccountNo());
        assertEquals("B001", opened.get(1).getAccountNo());
    }

    // ---- CRUD ----

    @Test
    public void addAndGet_roundTrips() {
        service.addAccount(active("A001", new BigDecimal("100.00")));
        Account loaded = service.getAccountByNo("A001");
        assertEquals("Owner-A001", loaded.getOwnerName());
        assertEquals(new BigDecimal("100.00"), loaded.getBalance());
    }

    @Test
    public void addAccount_forcesActive_andSetsOpenedDate() {
        Account a = new Account("A009", "X", new BigDecimal("0.00"), null, AccountStatus.FROZEN);
        service.addAccount(a);
        Account loaded = service.getAccountByNo("A009");
        assertEquals(AccountStatus.ACTIVATED, loaded.getStatus()); // 強制 ACTIVATED
        assertNotNull(loaded.getOpenedDate());                  // 自動補開戶日
    }

    @Test
    public void update_changesOwnerNameOnly_preservesOpenedDateAndStatus() {
        Date opened = new Date(0L);
        accountDAO.addAccount(new Account("A001", "Alice", new BigDecimal("100.00"), opened, AccountStatus.ACTIVATED));
        Account incoming = new Account("A001", "Alice Renamed", new BigDecimal("999.99"),
                new Date(), AccountStatus.FROZEN);
        service.updateAccount(incoming);
        Account after = service.getAccountByNo("A001");
        assertEquals("Alice Renamed", after.getOwnerName());
        assertEquals(opened, after.getOpenedDate());            // 開戶日不變
        assertEquals(AccountStatus.ACTIVATED, after.getStatus());  // 狀態不經 update 變更
        assertEquals(new BigDecimal("100.00"), after.getBalance()); // 餘額不經 update 變更
    }

    // ---- 狀態變更（changeStatus）----

    @Test
    public void changeStatus_activeToFrozen_logsStatusTx() {
        accountDAO.addAccount(active("A001", new BigDecimal("100.00")));
        service.changeStatus("A001", AccountStatus.FROZEN, "compliance hold");
        assertEquals(AccountStatus.FROZEN, service.getAccountByNo("A001").getStatus());
        List<Transaction> logs = transactionDAO.getTransactionsByAccount("A001");
        assertEquals(1, logs.size());
        assertEquals(TransactionType.STATUS, logs.get(0).getType());
        assertEquals(BigDecimal.ZERO, logs.get(0).getAmount());
        assertNotNull(logs.get(0).getNote());
        // item2：freeze note 應出現在 status log 的 note 內
        assertEquals(true, logs.get(0).getNote().contains("compliance hold"));
    }

    // item2：凍結未填 freeze note → FREEZE_NOTE_REQUIRED
    @Test(expected = IllegalArgumentException.class)
    public void changeStatus_freezeWithoutNote_throws() {
        accountDAO.addAccount(active("A001", new BigDecimal("100.00")));
        service.changeStatus("A001", AccountStatus.FROZEN, "  ");
    }

    // 本輪：解凍未填 activate note → ACTIVATE_NOTE_REQUIRED
    @Test(expected = IllegalArgumentException.class)
    public void changeStatus_activateWithoutNote_throws() {
        Account frozen = new Account("A001", "N", new BigDecimal("0.00"), new Date(), AccountStatus.FROZEN);
        accountDAO.addAccount(frozen);
        service.changeStatus("A001", AccountStatus.ACTIVATED, "  ");
    }

    // 本輪：解凍填了 activate note → OK，note 寫入 status log
    @Test
    public void changeStatus_activateWithNote_ok() {
        Account frozen = new Account("A001", "N", new BigDecimal("0.00"), new Date(), AccountStatus.FROZEN);
        accountDAO.addAccount(frozen);
        service.changeStatus("A001", AccountStatus.ACTIVATED, "verified, unfrozen");
        assertEquals(AccountStatus.ACTIVATED, service.getAccountByNo("A001").getStatus());
        List<Transaction> logs = transactionDAO.getTransactionsByAccount("A001");
        assertEquals(1, logs.size());
        assertEquals(true, logs.get(0).getNote().contains("verified, unfrozen"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void changeStatus_toClosed_rejected() {
        accountDAO.addAccount(active("A001", new BigDecimal("0.00")));
        service.changeStatus("A001", AccountStatus.CLOSED, "x"); // 應改用 closeAccount
    }

    // ---- 關戶（closeAccount）----

    @Test
    public void close_zeroBalance_setsClosedFieldsAndLogs() {
        accountDAO.addAccount(active("A001", new BigDecimal("0.00")));
        service.closeAccount("A001", "客戶要求結清");
        Account after = service.getAccountByNo("A001");
        assertEquals(AccountStatus.CLOSED, after.getStatus());
        assertNotNull(after.getClosedDate());
        assertEquals("客戶要求結清", after.getCloseNote());
        List<Transaction> logs = transactionDAO.getTransactionsByAccount("A001");
        assertEquals(1, logs.size());
        assertEquals(TransactionType.STATUS, logs.get(0).getType());
    }

    @Test(expected = IllegalStateException.class)
    public void close_nonZeroBalance_throws() {
        accountDAO.addAccount(active("A001", new BigDecimal("100.00")));
        service.closeAccount("A001", "note");
    }

    @Test(expected = IllegalArgumentException.class)
    public void close_missingNote_throws() {
        accountDAO.addAccount(active("A001", new BigDecimal("0.00")));
        service.closeAccount("A001", "   ");
    }

    @Test(expected = IllegalStateException.class)
    public void close_alreadyClosed_throws() {
        accountDAO.addAccount(active("A001", new BigDecimal("0.00")));
        service.closeAccount("A001", "first");
        service.closeAccount("A001", "second");
    }

    // ---- CLOSED 為終態（不可再變更狀態 / 不可編輯）----

    @Test(expected = IllegalStateException.class)
    public void closed_cannotChangeStatus() {
        accountDAO.addAccount(active("A001", new BigDecimal("0.00")));
        service.closeAccount("A001", "closed");
        service.changeStatus("A001", AccountStatus.ACTIVATED, "try reactivate");
    }

    @Test(expected = IllegalStateException.class)
    public void closed_cannotBeUpdated() {
        accountDAO.addAccount(active("A001", new BigDecimal("0.00")));
        service.closeAccount("A001", "closed");
        service.updateAccount(new Account("A001", "New Name", BigDecimal.ZERO, new Date(), AccountStatus.CLOSED));
    }

    // ---- openAccountsForNewUser（#3 phone/address 必填、#6 建 user+stamp owner）----

    @Test
    public void openForNewUser_createsUser_stampsOwner_setsContact() {
        List<Account> opened = service.openAccountsForNewUser(
                "u1", "password1", "Owner One", "0900-000-001", "Addr 1", Arrays.asList(AccountCategory.TWD));
        assertEquals(1, opened.size());
        Account a = opened.get(0);
        assertEquals("u1", a.getOwnerUsername());
        assertEquals("0900-000-001", a.getPhone());
        assertEquals("Addr 1", a.getAddress());
        assertNotNull(userService.findByUsername("u1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void openForNewUser_missingPhone_throws() {
        service.openAccountsForNewUser("u1", "password1", "N", "  ", "Addr", Arrays.asList(AccountCategory.TWD));
    }

    @Test(expected = IllegalArgumentException.class)
    public void openForNewUser_missingAddress_throws() {
        service.openAccountsForNewUser("u1", "password1", "N", "0900-000-001", "", Arrays.asList(AccountCategory.TWD));
    }

    // ---- #5 每電話每類型限一戶 ----

    @Test
    public void openForNewUser_sameOwner_oneTwdOneForeign_ok() {
        List<Account> opened = service.openAccountsForNewUser(
                "u1", "password1", "N", "0900-000-002", "Addr",
                Arrays.asList(AccountCategory.TWD, AccountCategory.FOREIGN));
        assertEquals(2, opened.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void openForNewUser_samePhoneSameCategory_secondFails() {
        service.openAccountsForNewUser("u1", "password1", "N", "0900-000-003", "Addr", Arrays.asList(AccountCategory.TWD));
        // 同電話 + 同類別（TWD）第二次 → DUPLICATE_ACCOUNT_FOR_PHONE
        service.openAccountsForNewUser("u2", "password1", "N", "0900-000-003", "Addr", Arrays.asList(AccountCategory.TWD));
    }

    // ---- #7 changeStatusBatch ----

    @Test
    public void changeStatusBatch_freezesActiveAccounts() {
        accountDAO.addAccount(active("A001", new BigDecimal("100.00")));
        accountDAO.addAccount(active("A002", new BigDecimal("200.00")));
        List<CloseAccountResult> results = service.changeStatusBatch(
                Arrays.asList("A001", "A002"), AccountStatus.FROZEN, "batch hold");
        assertEquals(2, results.size());
        assertEquals(AccountStatus.FROZEN, accountDAO.getAccountByNo("A001").getStatus());
        assertEquals(AccountStatus.FROZEN, accountDAO.getAccountByNo("A002").getStatus());
    }

    @Test
    public void changeStatusBatch_partialFailure_reported() {
        accountDAO.addAccount(active("A001", new BigDecimal("100.00")));
        List<CloseAccountResult> results = service.changeStatusBatch(
                Arrays.asList("A001", "NOPE"), AccountStatus.FROZEN, "batch hold");
        assertEquals(2, results.size());
        // 找出 NOPE 的結果應為失敗
        boolean nopeFailed = false;
        for (CloseAccountResult r : results) {
            if ("NOPE".equals(r.getAccountNo())) {
                nopeFailed = !r.isSuccess();
            }
        }
        assertEquals(true, nopeFailed);
    }

    // ---- #6 updateContactForOwner（同步該 user 所有帳戶）----

    @Test
    public void updateContactForOwner_updatesAllOwnedAccounts() {
        Account a1 = new Account("A001", "N", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED);
        a1.setOwnerUsername("u1");
        Account b1 = new Account("B001", "N", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED);
        b1.setOwnerUsername("u1");
        accountDAO.addAccount(a1);
        accountDAO.addAccount(b1);
        service.updateContactForOwner("u1", "0999-999-999", "New Addr");
        assertEquals("0999-999-999", accountDAO.getAccountByNo("A001").getPhone());
        assertEquals("New Addr", accountDAO.getAccountByNo("A001").getAddress());
        assertEquals("0999-999-999", accountDAO.getAccountByNo("B001").getPhone());
        assertEquals("New Addr", accountDAO.getAccountByNo("B001").getAddress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateContactForOwner_missingPhone_throws() {
        service.updateContactForOwner("u1", "", "Addr");
    }

    // ---- in-memory fakes ----

    static class FakeAccountDAO implements AccountDAO {
        private final Map<String, Account> store = new HashMap<String, Account>();

        public List<Account> getAllAccounts() {
            return new ArrayList<Account>(store.values());
        }

        public Account getAccountByNo(String accountNo) {
            return store.get(accountNo);
        }

        public List<Account> getAccountsByOwnerUsername(String ownerUsername) {
            List<Account> result = new ArrayList<Account>();
            for (Account a : store.values()) {
                if (ownerUsername != null && ownerUsername.equals(a.getOwnerUsername())) {
                    result.add(a);
                }
            }
            return result;
        }

        public List<Account> getAccountsByPhone(String phone) {
            List<Account> result = new ArrayList<Account>();
            for (Account a : store.values()) {
                if (phone != null && phone.equals(a.getPhone())) {
                    result.add(a);
                }
            }
            return result;
        }

        public void addAccount(Account account) {
            store.put(account.getAccountNo(), account);
        }

        public void updateAccount(Account account) {
            store.put(account.getAccountNo(), account);
        }

        public int getMaxSequence(char prefix) {
            int max = 0;
            for (String key : store.keySet()) {
                if (key.charAt(0) == prefix) {
                    int seq = Integer.parseInt(key.substring(1));
                    if (seq > max) max = seq;
                }
            }
            return max;
        }
    }

    static class FakeTransactionDAO implements TransactionDAO {
        private final List<Transaction> store = new ArrayList<Transaction>();

        public List<Transaction> getTransactionsByAccount(String accountNo) {
            List<Transaction> result = new ArrayList<Transaction>();
            for (Transaction t : store) {
                if (t.getAccount() != null && accountNo.equals(t.getAccount().getAccountNo())) {
                    result.add(t);
                }
            }
            return result;
        }

        public void addTransaction(Transaction transaction) {
            store.add(transaction);
        }
    }

    static class FakeUserDAO implements UserDAO {
        private final Map<String, User> store = new HashMap<String, User>();

        public User findByUsername(String username) {
            return store.get(username);
        }

        public void addUser(User user) {
            store.put(user.getUsername(), user);
        }

        public void updateUser(User user) {
            store.put(user.getUsername(), user);
        }

        public List<User> getAllUsers() {
            return new ArrayList<User>(store.values());
        }
    }
}
