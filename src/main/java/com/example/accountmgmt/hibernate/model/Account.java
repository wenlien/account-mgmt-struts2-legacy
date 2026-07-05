package com.example.accountmgmt.hibernate.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    /** 餘額用 BigDecimal 保證金額精度（S5）。 */
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Temporal(TemporalType.DATE)
    @Column(name = "opened_date")
    private Date openedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private AccountStatus status = AccountStatus.ACTIVATED;

    /** 關戶日：關帳戶時寫入，之後不再變動（開戶日 openedDate 一旦設定亦不可改）。 */
    @Temporal(TemporalType.DATE)
    @Column(name = "closed_date")
    private Date closedDate;

    /** 關戶註記：關帳戶時填寫，顯示於帳戶清單。 */
    @Column(name = "close_note", length = 255)
    private String closeNote;

    /** 帳戶擁有者的登入帳號（#6/#7：user 只能看/操作自己 ownerUsername 的帳戶；admin 建戶時指定）。 */
    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    /**
     * 擁有者 User（item3：owner name 中央化）。只讀（insertable/updatable=false），
     * 寫入仍透過 {@link #ownerUsername}；顯示 owner 名稱時參照 {@code owner.displayName}。
     * EAGER 以避免 JSP 渲染時 session 已關的 LazyInitializationException。
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_username", insertable = false, updatable = false)
    private User owner;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<Transaction>();

    /** 顯示用 owner 名稱：優先取中央 User.displayName，無則 fallback 到帳戶自身 ownerName。 */
    public String getOwnerDisplayName() {
        if (owner != null && owner.getDisplayName() != null && !owner.getDisplayName().isEmpty()) {
            return owner.getDisplayName();
        }
        return ownerName;
    }

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

    /** 依帳號前綴推導帳戶類別（A=TWD, B=FOREIGN）。 */
    public AccountCategory getCategory() {
        return AccountCategory.fromAccountNo(accountNo);
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

    public Date getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Date closedDate) {
        this.closedDate = closedDate;
    }

    public String getCloseNote() {
        return closeNote;
    }

    public void setCloseNote(String closeNote) {
        this.closeNote = closeNote;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
