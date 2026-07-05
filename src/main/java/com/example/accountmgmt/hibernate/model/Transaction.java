package com.example.accountmgmt.hibernate.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易紀錄 entity（新增，requirements.md R-A1.2 / design.md §3.1）。
 *
 * Account 1 — N Transaction。只記 DEPOSIT / WITHDRAW（D1）。
 * 同樣保留 javax.persistence.*（遷移雷）。
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tx_id")
    private Long txId;

    @ManyToOne
    @JoinColumn(name = "account_no", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private TransactionType type;

    /** 交易金額用 BigDecimal（S5）。 */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    /** 備註：轉帳說明 / 狀態變更細節（如 "ACTIVE -> CLOSED: <closeNote>"）。 */
    @Column(name = "note", length = 255)
    private String note;

    /** 交易後帳戶餘額（DEPOSIT/WITHDRAW 用；STATUS 為 null）。 */
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    /** 轉帳目標帳號（僅 transfer 的 WITHDRAW leg 填寫）。 */
    @Column(name = "target_account_no", length = 20)
    private String targetAccountNo;

    /** 狀態變更：變更前狀態（僅 STATUS 類型填寫）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 10)
    private AccountStatus fromStatus;

    /** 狀態變更：變更後狀態（僅 STATUS 類型填寫）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 10)
    private AccountStatus toStatus;

    public Transaction() {
    }

    public Transaction(Account account, TransactionType type, BigDecimal amount, Date createdAt) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    // Getters and setters

    public Long getTxId() {
        return txId;
    }

    public void setTxId(Long txId) {
        this.txId = txId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getTargetAccountNo() {
        return targetAccountNo;
    }

    public void setTargetAccountNo(String targetAccountNo) {
        this.targetAccountNo = targetAccountNo;
    }

    public AccountStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(AccountStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public AccountStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(AccountStatus toStatus) {
        this.toStatus = toStatus;
    }
}
