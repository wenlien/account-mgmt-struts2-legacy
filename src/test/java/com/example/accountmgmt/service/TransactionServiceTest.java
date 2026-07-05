package com.example.accountmgmt.service;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.dao.TransactionDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.service.impl.TransactionServiceImpl;
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
 * TransactionService 的 JUnit 4 測試（含 @Before）。
 *
 * 用 in-memory fake DAO，不依賴 Spring / Hibernate / MySQL。
 * 遷移雷：JUnit 4（@Before / org.junit.Test）→ Lab 5 練習遷移到 JUnit 5（@BeforeEach）。
 */
public class TransactionServiceTest {

    private TransactionServiceImpl service;
    private FakeAccountDAO accountDAO;
    private FakeTransactionDAO transactionDAO;

    @Before
    public void setUp() {
        accountDAO = new FakeAccountDAO();
        transactionDAO = new FakeTransactionDAO();
        Account a = new Account("A001", "Alice", new BigDecimal("1000.00"), new Date(), AccountStatus.ACTIVATED);
        accountDAO.addAccount(a);

        service = new TransactionServiceImpl();
        service.setAccountDAO(accountDAO);
        service.setTransactionDAO(transactionDAO);
    }

    @Test
    public void deposit_increasesBalance_andRecordsTransaction() {
        service.deposit("A001", new BigDecimal("250.00"));
        assertEquals(new BigDecimal("1250.00"), accountDAO.getAccountByNo("A001").getBalance());
        assertEquals(1, transactionDAO.getTransactionsByAccount("A001").size());
    }

    @Test
    public void withdraw_decreasesBalance_andRecordsTransaction() {
        service.withdraw("A001", new BigDecimal("300.00"));
        assertEquals(new BigDecimal("700.00"), accountDAO.getAccountByNo("A001").getBalance());
        assertEquals(1, transactionDAO.getTransactionsByAccount("A001").size());
    }

    @Test(expected = InsufficientBalanceException.class)
    public void withdraw_insufficientBalance_throws() {
        service.withdraw("A001", new BigDecimal("5000.00"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deposit_nonPositiveAmount_throws() {
        service.deposit("A001", new BigDecimal("0.00"));
    }

    @Test(expected = IllegalStateException.class)
    public void deposit_onFrozenAccount_throws() {
        Account frozen = new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.FROZEN);
        accountDAO.addAccount(frozen);
        service.deposit("A002", new BigDecimal("100.00"));
    }

    // ---- transfer ----

    @Test
    public void transfer_movesBalance_andRecordsTwoLegs() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("300.00"), null);
        assertEquals(new BigDecimal("700.00"), accountDAO.getAccountByNo("A001").getBalance());
        assertEquals(new BigDecimal("300.00"), accountDAO.getAccountByNo("A002").getBalance());
        assertEquals(1, transactionDAO.getTransactionsByAccount("A001").size()); // WITHDRAW leg
        assertEquals(1, transactionDAO.getTransactionsByAccount("A002").size()); // DEPOSIT leg
    }

    @Test(expected = InsufficientBalanceException.class)
    public void transfer_insufficientBalance_throws() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("5000.00"), null);
    }

    @Test(expected = IllegalStateException.class)
    public void transfer_frozenDestination_throws() {
        accountDAO.addAccount(new Account("A003", "Carol", new BigDecimal("0.00"), new Date(), AccountStatus.FROZEN));
        service.transfer("A001", "A003", new BigDecimal("100.00"), null);
    }

    @Test(expected = IllegalStateException.class)
    public void transfer_frozenSource_throws() {
        accountDAO.addAccount(new Account("A004", "Dan", new BigDecimal("500.00"), new Date(), AccountStatus.FROZEN));
        service.transfer("A004", "A001", new BigDecimal("100.00"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transfer_sameAccount_throws() {
        service.transfer("A001", "A001", new BigDecimal("100.00"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transfer_crossCategory_throws() {
        accountDAO.addAccount(new Account("B001", "Eve", new BigDecimal("500.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "B001", new BigDecimal("100.00"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transfer_nonexistentDestination_throws() {
        service.transfer("A001", "NOPE", new BigDecimal("100.00"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transfer_nonPositiveAmount_throws() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("0.00"), null);
    }

    @Test
    public void transfer_withNote_writesSameNoteToBothLegs() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("100.00"), "rent"); // 4 chars, within limit
        Transaction withdrawLeg = transactionDAO.getTransactionsByAccount("A001").get(0);
        Transaction depositLeg = transactionDAO.getTransactionsByAccount("A002").get(0);
        assertEquals("rent", withdrawLeg.getNote());
        assertEquals("rent", depositLeg.getNote());
    }

    @Test
    public void transfer_maxLengthNote_accepted() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("100.00"), "1234567"); // exactly 7 chars
        assertEquals("1234567", transactionDAO.getTransactionsByAccount("A002").get(0).getNote());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transfer_noteTooLong_throws() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("100.00"), "12345678"); // 8 chars, over limit
    }

    @Test
    public void transfer_blankNote_storedAsNull() {
        accountDAO.addAccount(new Account("A002", "Bob", new BigDecimal("0.00"), new Date(), AccountStatus.ACTIVATED));
        service.transfer("A001", "A002", new BigDecimal("100.00"), "   ");
        assertNull(transactionDAO.getTransactionsByAccount("A002").get(0).getNote());
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

        public void deleteAccount(String accountNo) {
            store.remove(accountNo);
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
}
