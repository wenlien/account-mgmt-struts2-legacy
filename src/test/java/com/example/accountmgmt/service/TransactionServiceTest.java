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
import static org.junit.Assert.assertTrue;

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
        Account a = new Account("A001", "Alice", new BigDecimal("1000.00"), new Date(), AccountStatus.ACTIVE);
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

    // ---- in-memory fakes ----

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
            assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0);
        }
    }
}
