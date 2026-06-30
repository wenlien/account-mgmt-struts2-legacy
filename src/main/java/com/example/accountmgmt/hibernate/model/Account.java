package com.example.accountmgmt.hibernate.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 帳戶 entity（re-skin 自原 bookstore 的 Book）。
 *
 * 對應 requirements.md R-A1.1 / design.md §3.1。
 * 刻意保留 javax.persistence.*（遷移雷：Java 21 / Spring Boot 3 要換成 jakarta.persistence.*）。
 */
@Entity
@Table(name = "accounts")
public class Account {

    /** 帳號為業務主鍵（String，非自動產生）。 */
    @Id
    @Column(name = "account_no", length = 20)
    private String accountNo;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    /** 餘額用 BigDecimal 保證金額精度（S5）。 */
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Temporal(TemporalType.DATE)
    @Column(name = "opened_date")
    private Date openedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<Transaction>();

    public Account() {
    }

    public Account(String accountNo, String ownerName, BigDecimal balance,
                   Date openedDate, AccountStatus status) {
        this.accountNo = accountNo;
        this.ownerName = ownerName;
        this.balance = balance;
        this.openedDate = openedDate;
        this.status = status;
    }

    // Getters and setters

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Date getOpenedDate() {
        return openedDate;
    }

    public void setOpenedDate(Date openedDate) {
        this.openedDate = openedDate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
