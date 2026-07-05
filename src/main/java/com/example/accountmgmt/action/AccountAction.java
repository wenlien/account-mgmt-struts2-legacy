package com.example.accountmgmt.action;

import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AccountCategory;
import com.example.accountmgmt.hibernate.model.AccountStatus;
import com.example.accountmgmt.security.SecurityUtil;
import com.example.accountmgmt.service.AccountService;
import com.example.accountmgmt.service.AuditService;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankValidationException;
import com.example.accountmgmt.service.CloseAccountResult;
import com.example.accountmgmt.service.CodedException;
import com.example.accountmgmt.service.TransactionService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 帳戶 CRUD + 生命週期（凍結 / 解凍 / 關戶）的 Struts2 Action。
 *
 * <p>授權（#7/#8）：list 依角色過濾（admin 全部、user 只看自己）；
 * edit/update 對非 admin 做 ownership 檢查；freeze/activate/close/open 屬 admin
 * （URL 層由 SecurityConfig 限制，Action 層再記稽核）。所有 mutating 操作寫 audit（#9）。</p>
 */
public class AccountAction extends ActionSupport {

    /** 額外的 result：回列表頁（非 redirect），用於顯示操作錯誤。 */
    private static final String LIST = "list";

    private Account account;
    private List<Account> accountList;
    private String accountNo;
    private String closeNote;
    /** item2：凍結註記（freeze note，必填）。 */
    private String freezeNote;
    /** 本輪：解凍註記（activate note，必填）。 */
    private String activateNote;
    private String ownerName;
    private String phone;
    private String address;
    private String[] categories;
    private String[] accountNos;
    private List<Account> closeTargets;
    /** item2：批次凍結表單的目標帳號清單。 */
    private List<Account> freezeTargets;
    /** 本輪：批次解凍表單的目標帳號清單。 */
    private List<Account> activateTargets;
    private BigDecimal twdDeposit;
    private BigDecimal foreignDeposit;
    /** #6：開戶同時建立登入帳號。 */
    private String username;
    private String password;
    /** #2：密碼二次確認。 */
    private String confirmPassword;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuditService auditService;

    public String list() {
        // #7：admin 看全部；一般 user 只看自己名下帳戶。
        if (SecurityUtil.isAdmin()) {
            accountList = accountService.getAllAccounts();
        } else {
            accountList = accountService.getAccountsByOwner(SecurityUtil.currentUsername());
        }
        return SUCCESS;
    }

    public String add() {
        return INPUT;
    }

    public String save() {
        accountService.addAccount(account);
        return SUCCESS;
    }

    /** 顯示凍結表單（item2：需填 freeze note）。 */
    public String freezeForm() {
        account = accountService.getAccountByNo(accountNo);
        if (account == null) {
            addActionError("Account not found: " + accountNo);
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        return INPUT;
    }

    /** 凍結（ACTIVE → FROZEN），需填 freeze note（item2）。 */
    public String freeze() {
        if (freezeNote == null || freezeNote.trim().isEmpty()) {
            addActionError(CodedException.format(BankErrorCode.FREEZE_NOTE_REQUIRED, "Freeze note is required"));
            account = accountService.getAccountByNo(accountNo);
            return INPUT;
        }
        return applyStatus(AccountStatus.FROZEN, "FREEZE", freezeNote);
    }

    /** 顯示解凍表單（本輪：需填 activate note）。 */
    public String activateForm() {
        account = accountService.getAccountByNo(accountNo);
        if (account == null) {
            addActionError("Account not found: " + accountNo);
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        return INPUT;
    }

    /** 解凍（FROZEN → ACTIVE），需填 activate note（本輪）。 */
    public String activate() {
        if (activateNote == null || activateNote.trim().isEmpty()) {
            addActionError(CodedException.format(BankErrorCode.ACTIVATE_NOTE_REQUIRED, "Activate note is required"));
            account = accountService.getAccountByNo(accountNo);
            return INPUT;
        }
        return applyStatus(AccountStatus.ACTIVATED, "ACTIVATE", activateNote);
    }

    /** 顯示關戶表單（需填註記）。 */
    public String closeForm() {
        account = accountService.getAccountByNo(accountNo);
        if (account == null) {
            addActionError("Account not found: " + accountNo);
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        return INPUT;
    }

    /** 關戶（餘額須 0 + 註記）。 */
    public String close() {
        String actor = SecurityUtil.currentUsername();
        try {
            if (accountNos != null && accountNos.length > 0) {
                accountNo = accountNos[0]; // 單筆模式從 hidden field
            }
            accountService.closeAccount(accountNo, closeNote);
            auditService.logSuccess(actor, "CLOSE", accountNo, "closed: " + closeNote);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "CLOSE", accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            account = accountService.getAccountByNo(accountNo);
            return INPUT;
        }
        return SUCCESS;
    }

    /** 新版開戶：選類別（可多選），帳號自動產生，並同時建立登入帳號（#6）。 */
    public String openAccount() {
        String actor = SecurityUtil.currentUsername();
        if (ownerName == null || ownerName.trim().isEmpty()) {
            addActionError(CodedException.format(BankErrorCode.OWNER_NAME_REQUIRED, "Owner Name is required"));
            return INPUT;
        }
        if (categories == null || categories.length == 0) {
            addActionError(CodedException.format(BankErrorCode.CATEGORY_REQUIRED,
                    "Please select at least one account category"));
            return INPUT;
        }
        // #2：密碼需輸入兩次且一致。
        if (password == null || !password.equals(confirmPassword)) {
            addActionError(CodedException.format(BankErrorCode.PASSWORD_MISMATCH,
                    "Password and confirmation do not match"));
            return INPUT;
        }
        try {
            List<AccountCategory> cats = new ArrayList<AccountCategory>();
            for (String c : categories) {
                cats.add(AccountCategory.valueOf(c));
            }
            // #6：建立 user + 開戶（同一交易，原子）。
            List<Account> opened = accountService.openAccountsForNewUser(
                    username, password, ownerName.trim(), phone, address, cats);
            StringBuilder openedNos = new StringBuilder();
            for (Account a : opened) {
                if (openedNos.length() > 0) openedNos.append(",");
                openedNos.append(a.getAccountNo());
                // 依類別取對應的初始存款金額
                BigDecimal deposit = BigDecimal.ZERO;
                if (a.getAccountNo().charAt(0) == 'A' && twdDeposit != null && twdDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    deposit = twdDeposit;
                } else if (a.getAccountNo().charAt(0) == 'B' && foreignDeposit != null && foreignDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    deposit = foreignDeposit;
                }
                if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                    transactionService.deposit(a.getAccountNo(), deposit);
                }
                addActionMessage("Opened: " + a.getAccountNo() + " (" + a.getCategory() + ")"
                        + (deposit.compareTo(BigDecimal.ZERO) > 0 ? " with deposit " + deposit : ""));
            }
            auditService.logSuccess(actor, "OPEN_ACCOUNT", openedNos.toString(),
                    "user=" + username + " opened " + opened.size() + " account(s)");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "OPEN_ACCOUNT", null, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            return INPUT;
        }
        return SUCCESS;
    }

    /** 批次關戶表單（從 accountList 勾選多筆後按 Close Selected）。 */
    public String batchCloseForm() {
        if (accountNos == null || accountNos.length == 0) {
            addActionError("Please select at least one account to close");
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        closeTargets = new ArrayList<Account>();
        for (String no : accountNos) {
            Account a = accountService.getAccountByNo(no);
            if (a != null) closeTargets.add(a);
        }
        return INPUT;
    }

    /** 批次關戶執行。 */
    public String closeAccounts() {
        String actor = SecurityUtil.currentUsername();
        if (accountNos == null || accountNos.length == 0) {
            addActionError("No accounts selected");
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        if (closeNote == null || closeNote.trim().isEmpty()) {
            addActionError("Close note is required");
            closeTargets = new ArrayList<Account>();
            for (String no : accountNos) {
                Account a = accountService.getAccountByNo(no);
                if (a != null) closeTargets.add(a);
            }
            return INPUT;
        }
        List<String> nos = new ArrayList<String>();
        for (String no : accountNos) nos.add(no);
        List<CloseAccountResult> results = accountService.closeAccounts(nos, closeNote);
        for (CloseAccountResult r : results) {
            if (r.isSuccess()) {
                auditService.logSuccess(actor, "CLOSE", r.getAccountNo(), "batch close: " + closeNote);
                addActionMessage("Closed: " + r.getAccountNo());
            } else {
                auditService.logFailure(actor, "CLOSE", r.getAccountNo(), null,
                        "batch close failed: " + r.getReason());
                addActionError("Failed to close " + r.getAccountNo() + ": " + r.getReason());
            }
        }
        accountList = accountService.getAllAccounts();
        return LIST;
    }

    /** 批次凍結表單（item2：勾選多筆後填一次 freeze note）。 */
    public String batchFreezeForm() {
        if (accountNos == null || accountNos.length == 0) {
            addActionError("Please select at least one account to freeze");
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        freezeTargets = new ArrayList<Account>();
        for (String no : accountNos) {
            Account a = accountService.getAccountByNo(no);
            if (a != null) freezeTargets.add(a);
        }
        return INPUT;
    }

    /** 批次凍結（item7 + item2：需 freeze note）。 */
    public String batchFreeze() {
        if (freezeNote == null || freezeNote.trim().isEmpty()) {
            addActionError(CodedException.format(BankErrorCode.FREEZE_NOTE_REQUIRED, "Freeze note is required"));
            freezeTargets = new ArrayList<Account>();
            if (accountNos != null) {
                for (String no : accountNos) {
                    Account a = accountService.getAccountByNo(no);
                    if (a != null) freezeTargets.add(a);
                }
            }
            return INPUT;
        }
        return batchChangeStatus(AccountStatus.FROZEN, "FREEZE", freezeNote);
    }

    /** 批次解凍表單（本輪：勾選多筆後填一次 activate note）。 */
    public String batchActivateForm() {
        if (accountNos == null || accountNos.length == 0) {
            addActionError("Please select at least one account to activate");
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        activateTargets = new ArrayList<Account>();
        for (String no : accountNos) {
            Account a = accountService.getAccountByNo(no);
            if (a != null) activateTargets.add(a);
        }
        return INPUT;
    }

    /** 批次解凍（item7 + 本輪：需 activate note）。 */
    public String batchActivate() {
        if (activateNote == null || activateNote.trim().isEmpty()) {
            addActionError(CodedException.format(BankErrorCode.ACTIVATE_NOTE_REQUIRED, "Activate note is required"));
            activateTargets = new ArrayList<Account>();
            if (accountNos != null) {
                for (String no : accountNos) {
                    Account a = accountService.getAccountByNo(no);
                    if (a != null) activateTargets.add(a);
                }
            }
            return INPUT;
        }
        return batchChangeStatus(AccountStatus.ACTIVATED, "ACTIVATE", activateNote);
    }

    private String batchChangeStatus(AccountStatus newStatus, String auditAction, String note) {
        String actor = SecurityUtil.currentUsername();
        if (accountNos == null || accountNos.length == 0) {
            addActionError("Please select at least one account");
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        List<String> nos = new ArrayList<String>();
        for (String no : accountNos) nos.add(no);
        List<CloseAccountResult> results = accountService.changeStatusBatch(nos, newStatus, note);
        for (CloseAccountResult r : results) {
            if (r.isSuccess()) {
                auditService.logSuccess(actor, auditAction, r.getAccountNo(), "batch status -> " + newStatus);
                addActionMessage(auditAction + " OK: " + r.getAccountNo());
            } else {
                auditService.logFailure(actor, auditAction, r.getAccountNo(), null,
                        "batch " + auditAction + " failed: " + r.getReason());
                addActionError("Failed " + auditAction + " " + r.getAccountNo() + ": " + r.getReason());
            }
        }
        accountList = accountService.getAllAccounts();
        return LIST;
    }

    private String applyStatus(AccountStatus newStatus, String auditAction, String note) {
        String actor = SecurityUtil.currentUsername();
        try {
            accountService.changeStatus(accountNo, newStatus, note);
            String detail = "status -> " + newStatus
                    + (note != null && !note.trim().isEmpty() ? " (" + note.trim() + ")" : "");
            auditService.logSuccess(actor, auditAction, accountNo, detail);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, auditAction, accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            accountList = accountService.getAllAccounts();
            return LIST;
        }
        return SUCCESS;
    }

    // Getters and setters

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCloseNote() {
        return closeNote;
    }

    public void setCloseNote(String closeNote) {
        this.closeNote = closeNote;
    }

    public String getFreezeNote() {
        return freezeNote;
    }

    public void setFreezeNote(String freezeNote) {
        this.freezeNote = freezeNote;
    }

    public List<Account> getFreezeTargets() {
        return freezeTargets;
    }

    public void setFreezeTargets(List<Account> freezeTargets) {
        this.freezeTargets = freezeTargets;
    }

    public String getActivateNote() {
        return activateNote;
    }

    public void setActivateNote(String activateNote) {
        this.activateNote = activateNote;
    }

    public List<Account> getActivateTargets() {
        return activateTargets;
    }

    public void setActivateTargets(List<Account> activateTargets) {
        this.activateTargets = activateTargets;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public String[] getAccountNos() {
        return accountNos;
    }

    public void setAccountNos(String[] accountNos) {
        this.accountNos = accountNos;
    }

    public List<Account> getCloseTargets() {
        return closeTargets;
    }

    public void setCloseTargets(List<Account> closeTargets) {
        this.closeTargets = closeTargets;
    }

    public BigDecimal getTwdDeposit() {
        return twdDeposit;
    }

    public void setTwdDeposit(BigDecimal twdDeposit) {
        this.twdDeposit = twdDeposit;
    }

    public BigDecimal getForeignDeposit() {
        return foreignDeposit;
    }

    public void setForeignDeposit(BigDecimal foreignDeposit) {
        this.foreignDeposit = foreignDeposit;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
