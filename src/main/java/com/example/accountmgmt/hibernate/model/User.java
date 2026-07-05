package com.example.accountmgmt.hibernate.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * 系統登入使用者 entity（#6）。
 *
 * <p>一個 owner 對應一個登入帳號；該使用者名下可有多個銀行帳戶
 * （Account.ownerUsername 指回此 username）。密碼以 BCrypt 雜湊存於
 * {@code passwordHash}，絕不存明文。admin 走 in-memory 認證，不落此表。</p>
 *
 * <p>保留 javax.persistence.*（遷移雷：Jakarta 化時改 jakarta.persistence.*）。</p>
 */
@Entity
@Table(name = "users")
public class User {

    /** 登入帳號（業務主鍵）。 */
    @Id
    @Column(name = "username", length = 50)
    private String username;

    /** BCrypt 雜湊後的密碼（不存明文）。 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** 角色：DB 使用者一律 ROLE_USER（admin 為 in-memory ROLE_ADMIN，不在此表）。 */
    @Column(name = "role", nullable = false, length = 20)
    private String role = "ROLE_USER";

    /** 顯示名稱（對應 owner 姓名）。 */
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    public User() {
    }

    public User(String username, String passwordHash, String role, String displayName, Date createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
