package com.example.accountmgmt.service.impl;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.dao.TransactionDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountCategory;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.hibernate.model.TransactionType;
import com.example.accountmgmt.service.AccountService;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankStateException;
import com.example.accountmgmt.service.BankValidationException;
import com.example.accountmgmt.service.CloseAccountResult;
import com.example.accountmgmt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDAO accountDAO;

    /** 狀態變更需寫入交易 log，故 AccountService 也依賴 TransactionDAO。 */
    @Autowired
    private TransactionDAO transactionDAO;

    /** #6：開戶同時建立登入帳號，故依賴 UserService（同一交易內原子）。 */
    @Autowired
    private UserService userService;

    /** 供測試 / XML setter 注入用。 */
    public void setAccountDAO(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    public void setTransactionDAO(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<Account> getAllAccounts() {
        return accountDAO.getAllAccounts();
    }

    @Override
    public Account getAccountByNo(String accountNo) {
        return accountDAO.getAccountByNo(accountNo);
    }

    @Override
    @Transactional
    public void addAccount(Account account) {
        // 新帳戶一律 ACTIVE；開戶日未給則設今日；關戶欄位清空。
        account.setStatus(AccountStatus.ACTIVATED);
        if (account.getOpenedDate() == null) {
            account.setOpenedDate(new Date());
        }
        account.setClosedDate(null);
        account.setCloseNote(null);
        accountDAO.addAccount(account);
    }

    @Override
    @Transactional
    public Account openAccount(String ownerName, String phone, String address, AccountCategory category, String ownerUsername) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.OWNER_NAME_REQUIRED, "ownerName is required");
        }
        if (category == null) {
            throw new BankValidationException(BankErrorCode.CATEGORY_REQUIRED, "category is required");
        }
        // #5：電話為 identity，同一電話每類型（A/B）只能有一個非 CLOSED 帳戶。
        if (phone != null && !phone.trim().isEmpty()) {
            for (Account existing : accountDAO.getAccountsByPhone(phone.trim())) {
                if (existing.getStatus() != AccountStatus.CLOSED
                        && existing.getAccountNo() != null
                        && existing.getAccountNo().charAt(0) == category.getPrefix()) {
                    throw new BankValidationException(BankErrorCode.DUPLICATE_ACCOUNT_FOR_PHONE,
                            "Phone " + phone.trim() + " already has an active " + category
                                    + " account (" + existing.getAccountNo() + ")");
                }
            }
        }
        int seq = accountDAO.getMaxSequence(category.getPrefix()) + 1;
        if (seq > 999) {
            throw new BankStateException(BankErrorCode.ACCOUNT_NUMBER_OVERFLOW,
                    "Account number overflow for category " + category);
        }
        String accountNo = String.format("%c%03d", category.getPrefix(), seq);
        Account account = new Account(accountNo, ownerName.trim(), BigDecimal.ZERO, new Date(), AccountStatus.ACTIVATED);
        account.setPhone(phone != null ? phone.trim() : null);
        account.setAddress(address != null ? address.trim() : null);
        account.setOwnerUsername(ownerUsername);
        accountDAO.addAccount(account);
        // 開戶 status log: INITIATING → ACTIVATED
        Transaction statusTx = new Transaction(account, TransactionType.STATUS, BigDecimal.ZERO, new Date());
        statusTx.setToStatus(AccountStatus.ACTIVATED);
        statusTx.setNote("INITIATING -> ACTIVATED: Account opened");
        transactionDAO.addTransaction(statusTx);
        return account;
    }

    @Override
    @Transactional
    public List<Account> openAccounts(String ownerName, String phone, String address,
                                      List<AccountCategory> categories, String ownerUsername) {
        if (categories == null || categories.isEmpty()) {
            throw new BankValidationException(BankErrorCode.CATEGORY_REQUIRED,
                    "At least one category is required");
        }
        List<Account> opened = new ArrayList<Account>();
        for (AccountCategory cat : categories) {
            opened.add(openAccount(ownerName, phone, address, cat, ownerUsername));
        }
        return opened;
    }

    @Override
    @Transactional
    public List<Account> openAccountsForNewUser(String username, String rawPassword, String ownerName,
                                                String phone, String address, List<AccountCategory> categories) {
        // #3：開戶時電話、地址必填。
        if (phone == null || phone.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.PHONE_REQUIRED, "Phone is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.ADDRESS_REQUIRED, "Address is required");
        }
        // 先建立登入帳號（含 username 必填/唯一 + 密碼長度驗證，BCrypt 雜湊）；
        // 與開戶同一交易，任一步失敗整筆 rollback（不會留孤兒 user）。
        String u = username == null ? null : username.trim();
        userService.createUser(u, rawPassword, ownerName, "ROLE_USER");
        return openAccounts(ownerName, phone, address, categories, u);
    }

    @Override
    @Transactional
    public List<Account> getAccountsByOwner(String ownerUsername) {
        return accountDAO.getAccountsByOwnerUsername(ownerUsername);
    }

    @Override
    @Transactional
    public void updateContactForOwner(String ownerUsername, String phone, String address) {
        // #6：電話為 identity，phone/address 變更套用到該 user 名下所有帳戶（保持一致）。
        if (phone == null || phone.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.PHONE_REQUIRED, "Phone is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.ADDRESS_REQUIRED, "Address is required");
        }
        List<Account> accounts = accountDAO.getAccountsByOwnerUsername(ownerUsername);
        for (Account a : accounts) {
            a.setPhone(phone.trim());
            a.setAddress(address.trim());
            accountDAO.updateAccount(a);
        }
    }

    @Override
    @Transactional
    public void updateAccount(Account incoming) {
        Account existing = accountDAO.getAccountByNo(incoming.getAccountNo());
        if (existing == null) {
            throw new BankValidationException(BankErrorCode.ACCOUNT_NOT_FOUND,
                    "Account not found: " + incoming.getAccountNo());
        }
        if (existing.getStatus() == AccountStatus.CLOSED) {
            throw new BankStateException(BankErrorCode.ACCOUNT_CLOSED,
                    "Closed account cannot be modified: " + existing.getAccountNo());
        }
        // 僅允許改戶名；openedDate（開戶日一旦設定不可改）、status、balance、
        // closedDate、closeNote 一律保留既有值。
        existing.setOwnerName(incoming.getOwnerName());
        accountDAO.updateAccount(existing);
    }

    @Override
    @Transactional
    public void changeStatus(String accountNo, AccountStatus newStatus, String note) {
        if (newStatus == null) {
            throw new BankValidationException(BankErrorCode.INVALID_STATUS_CHANGE, "newStatus is required");
        }
        if (newStatus == AccountStatus.CLOSED) {
            throw new BankValidationException(BankErrorCode.INVALID_STATUS_CHANGE,
                    "Use closeAccount to close (balance check + note required)");
        }
        // item2 + 本輪：狀態變更一律需填註記（凍結 freeze note / 解凍 activate note）。
        if (note == null || note.trim().isEmpty()) {
            if (newStatus == AccountStatus.FROZEN) {
                throw new BankValidationException(BankErrorCode.FREEZE_NOTE_REQUIRED, "Freeze note is required");
            }
            throw new BankValidationException(BankErrorCode.ACTIVATE_NOTE_REQUIRED, "Activate note is required");
        }
        Account account = loadAccount(accountNo);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BankStateException(BankErrorCode.ACCOUNT_CLOSED,
                    "Account is CLOSED (terminal), status cannot be changed: " + accountNo);
        }
        AccountStatus old = account.getStatus();
        if (old == newStatus) {
            return; // 無變更，不記 log
        }
        account.setStatus(newStatus);
        accountDAO.updateAccount(account);
        logStatusChange(account, old, newStatus, note == null ? null : note.trim());
    }

    @Override
    @Transactional
    public void closeAccount(String accountNo, String note) {
        if (note == null || note.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.CLOSE_NOTE_REQUIRED, "Close note is required");
        }
        Account account = loadAccount(accountNo);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BankStateException(BankErrorCode.ACCOUNT_CLOSED,
                    "Account is already closed: " + accountNo);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BankStateException(BankErrorCode.CLOSE_BALANCE_NOT_ZERO,
                    "Balance must be zero to close account " + accountNo
                            + " (current balance: " + account.getBalance() + ")");
        }
        AccountStatus old = account.getStatus();
        account.setStatus(AccountStatus.CLOSED);
        account.setClosedDate(new Date());
        account.setCloseNote(note);
        accountDAO.updateAccount(account);
        logStatusChange(account, old, AccountStatus.CLOSED, note);
    }

    @Override
    @Transactional
    public List<CloseAccountResult> closeAccounts(List<String> accountNos, String note) {
        if (accountNos == null || accountNos.isEmpty()) {
            throw new BankValidationException(BankErrorCode.ACCOUNT_NOT_FOUND,
                    "At least one account number is required");
        }
        if (note == null || note.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.CLOSE_NOTE_REQUIRED, "Close note is required");
        }
        List<CloseAccountResult> results = new ArrayList<CloseAccountResult>();
        for (String no : accountNos) {
            try {
                closeAccount(no, note);
                results.add(new CloseAccountResult(no, true, null));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                results.add(new CloseAccountResult(no, false, ex.getMessage()));
            }
        }
        return results;
    }

    @Override
    @Transactional
    public List<CloseAccountResult> changeStatusBatch(List<String> accountNos, AccountStatus newStatus, String note) {
        if (accountNos == null || accountNos.isEmpty()) {
            throw new BankValidationException(BankErrorCode.ACCOUNT_NOT_FOUND,
                    "At least one account number is required");
        }
        // item2 + 本輪：批次凍結 / 解凍同樣需要 note（套用到每一筆）。
        if (note == null || note.trim().isEmpty()) {
            if (newStatus == AccountStatus.FROZEN) {
                throw new BankValidationException(BankErrorCode.FREEZE_NOTE_REQUIRED, "Freeze note is required");
            }
            throw new BankValidationException(BankErrorCode.ACTIVATE_NOTE_REQUIRED, "Activate note is required");
        }
        List<CloseAccountResult> results = new ArrayList<CloseAccountResult>();
        for (String no : accountNos) {
            try {
                changeStatus(no, newStatus, note);
                results.add(new CloseAccountResult(no, true, null));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                results.add(new CloseAccountResult(no, false, ex.getMessage()));
            }
        }
        return results;
    }

    // ---- helpers ----

    private Account loadAccount(String accountNo) {
        Account account = accountDAO.getAccountByNo(accountNo);
        if (account == null) {
            throw new BankValidationException(BankErrorCode.ACCOUNT_NOT_FOUND,
                    "Account not found: " + accountNo);
        }
        return account;
    }

    /** 把狀態變更寫成一筆 STATUS 交易（amount = 0，帶 fromStatus/toStatus，細節放 note）。 */
    private void logStatusChange(Account account, AccountStatus from, AccountStatus to, String note) {
        String detail = from + " -> " + to;
        if (note != null && !note.trim().isEmpty()) {
            detail += ": " + note;
        }
        Transaction tx = new Transaction(account, TransactionType.STATUS, BigDecimal.ZERO, new Date());
        tx.setFromStatus(from);
        tx.setToStatus(to);
        tx.setNote(detail);
        transactionDAO.addTransaction(tx);
    }
}
