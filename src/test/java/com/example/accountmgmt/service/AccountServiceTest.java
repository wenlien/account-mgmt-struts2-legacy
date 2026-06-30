package com.example.accountmgmt.service;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.service.impl.AccountServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * AccountService 的 JUnit 4 測試（含 @Before）。
 * 驗證 CRUD 委派給 DAO；用 in-memory fake DAO，不依賴 Spring / DB。
 */
public class AccountServiceTest {

    private AccountServiceImpl service;
    private FakeAccountDAO accountDAO;

    @Before
    public void setUp() {
        accountDAO = new FakeAccountDAO();
        service = new AccountServiceImpl();
        service.setAccountDAO(accountDAO);
    }

    @Test
    public void addAndGet_roundTrips() {
        Account a = new Account("A001", "Alice", new BigDecimal("100.00"), new Date(), AccountStatus.ACTIVE);
        service.addAccount(a);
        Account loaded = service.getAccountByNo("A001");
        assertEquals("Alice", loaded.getOwnerName());
        assertEquals(new BigDecimal("100.00"), loaded.getBalance());
    }

    @Test
    public void getAllAccounts_returnsAll() {
        service.addAccount(new Account("A001", "Alice", BigDecimal.ZERO, new Date(), AccountStatus.ACTIVE));
        service.addAccount(new Account("A002", "Bob", BigDecimal.ZERO, new Date(), AccountStatus.ACTIVE));
        assertEquals(2, service.getAllAccounts().size());
    }

    @Test
    public void delete_removesAccount() {
        service.addAccount(new Account("A001", "Alice", BigDecimal.ZERO, new Date(), AccountStatus.ACTIVE));
        service.deleteAccount("A001");
        assertNull(service.getAccountByNo("A001"));
    }

    static class FakeAccountDAO implements AccountDAO {
        private final Map<String, Account> store = new HashMap<String, Account>();

        public List<Account> getAllAccounts() {
            return new ArrayList<Account>(store.values());
        }

        public Account getAccountByNo(String accountNo) {
            return store.get(accountNo);
        }

        public void addAccount(Account account) {
            store.put(account.getAccountNo(), account);
        }

        public void updateAccount(Account account) {
            store.put(account.getAccountNo(), account);
        }

        public void deleteAccount(String accountNo) {
            store.remove(accountNo);
        }
    }
}
