package com.example.accountmgmt.action;

import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.Transaction;
import com.example.accountmgmt.security.SecurityUtil;
import com.example.accountmgmt.service.AccountService;
import com.example.accountmgmt.service.AuditService;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankValidationException;
import com.example.accountmgmt.service.CodedException;
import com.example.accountmgmt.service.InsufficientBalanceException;
import com.example.accountmgmt.service.TransactionService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

/**
 * 存款 / 提款 / 轉帳 / 查交易紀錄的 Struts2 Action。
 *
 * <p>授權（item1）：deposit / withdraw / transfer 只有帳戶擁有者本人可執行；
 * <b>admin 只能檢視紀錄，不可代做任何交易</b>（Action 層擋下，回 {@code [E6003]}）。
 * 一般 user 亦只能操作自己名下帳戶（ownership 檢查，回 {@code [E6002]}）。所有交易寫 audit（#9）。</p>
 */
public class TransactionAction extends ActionSupport {

    private String accountNo;
    private String toAccountNo;
    private BigDecimal depositAmount;
    private BigDecimal withdrawAmount;
    private BigDecimal transferAmount;
    private String transferNote;
    private List<Transaction> transactionList;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuditService auditService;

    public String list() {
        // #7：非 admin 只能查看自己名下帳戶的交易。
        if (!SecurityUtil.isAdmin() && !isOwnedByCurrentUser(accountNo)) {
            addActionError(CodedException.format(BankErrorCode.NOT_ACCOUNT_OWNER,
                    "You can only view your own account's transactions"));
            transactionList = null;
            return SUCCESS;
        }
        transactionList = transactionService.getTransactions(accountNo);
        return SUCCESS;
    }

    public String deposit() {
        String actor = SecurityUtil.currentUsername();
        try {
            // item1：admin 只能看紀錄，不可代做交易（存款）。
            if (SecurityUtil.isAdmin()) {
                throw new BankValidationException(BankErrorCode.ADMIN_CANNOT_TRANSACT,
                        "Admin can only view records; deposits must be done by the account owner");
            }
            // #8：user 只能對自己名下帳戶存款（防繞過）。
            if (!isOwnedByCurrentUser(accountNo)) {
                throw new BankValidationException(BankErrorCode.NOT_ACCOUNT_OWNER,
                        "You can only deposit to your own account");
            }
            transactionService.deposit(accountNo, depositAmount);
            auditService.logSuccess(actor, "DEPOSIT", accountNo, "amount=" + depositAmount);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "DEPOSIT", accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            transactionList = transactionService.getTransactions(accountNo);
            return INPUT;
        }
        return SUCCESS;
    }

    public String withdraw() {
        String actor = SecurityUtil.currentUsername();
        try {
            // item1：admin 只能看紀錄，不可代做交易（提款）。
            if (SecurityUtil.isAdmin()) {
                throw new BankValidationException(BankErrorCode.ADMIN_CANNOT_TRANSACT,
                        "Admin can only view records; withdrawals must be done by the account owner");
            }
            // #8：user 只能對自己名下帳戶提款（防繞過）。
            if (!isOwnedByCurrentUser(accountNo)) {
                throw new BankValidationException(BankErrorCode.NOT_ACCOUNT_OWNER,
                        "You can only withdraw from your own account");
            }
            transactionService.withdraw(accountNo, withdrawAmount);
            auditService.logSuccess(actor, "WITHDRAW", accountNo, "amount=" + withdrawAmount);
        } catch (InsufficientBalanceException ex) {
            auditService.logFailure(actor, "WITHDRAW", accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            transactionList = transactionService.getTransactions(accountNo);
            return INPUT;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "WITHDRAW", accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            transactionList = transactionService.getTransactions(accountNo);
            return INPUT;
        }
        return SUCCESS;
    }

    public String transfer() {
        String actor = SecurityUtil.currentUsername();
        try {
            // item1：admin 只能看紀錄，不可代做交易（轉帳）。
            if (SecurityUtil.isAdmin()) {
                throw new BankValidationException(BankErrorCode.ADMIN_CANNOT_TRANSACT,
                        "Admin can only view records; transfers must be done by the account owner");
            }
            // #8：一般 user 只能從自己名下帳戶轉出（防繞過）。
            if (!isOwnedByCurrentUser(accountNo)) {
                throw new BankValidationException(BankErrorCode.NOT_ACCOUNT_OWNER,
                        "You can only transfer from your own account");
            }
            transactionService.transfer(accountNo, toAccountNo, transferAmount, transferNote);
            auditService.logSuccess(actor, "TRANSFER", accountNo,
                    "-> " + toAccountNo + " amount=" + transferAmount
                            + (transferNote != null && !transferNote.trim().isEmpty() ? " note=" + transferNote : ""));
        } catch (InsufficientBalanceException ex) {
            auditService.logFailure(actor, "TRANSFER", accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            transactionList = transactionService.getTransactions(accountNo);
            return INPUT;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "TRANSFER", accountNo, CodedException.codeOf(ex), ex.getMessage());
            addActionError(ex.getMessage());
            transactionList = transactionService.getTransactions(accountNo);
            return INPUT;
        }
        return SUCCESS;
    }

    private boolean isOwnedByCurrentUser(String acctNo) {
        String me = SecurityUtil.currentUsername();
        if (me == null || acctNo == null) {
            return false;
        }
        Account acct = accountService.getAccountByNo(acctNo);
        return acct != null && me.equals(acct.getOwnerUsername());
    }

    // Getters and setters

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getToAccountNo() {
        return toAccountNo;
    }

    public void setToAccountNo(String toAccountNo) {
        this.toAccountNo = toAccountNo;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getWithdrawAmount() {
        return withdrawAmount;
    }

    public void setWithdrawAmount(BigDecimal withdrawAmount) {
        this.withdrawAmount = withdrawAmount;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public String getTransferNote() {
        return transferNote;
    }

    public void setTransferNote(String transferNote) {
        this.transferNote = transferNote;
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }
}
