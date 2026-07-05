package com.example.accountmgmt.service;

import com.example.accountmgmt.hibernate.model.User;

import java.util.List;

/**
 * 系統使用者服務（#6）：建立登入帳號（BCrypt 雜湊）、改密碼、查詢。
 */
public interface UserService {

    /**
     * 建立登入使用者。username 必填且唯一、密碼最少 8 字元、以 BCrypt 雜湊儲存。
     *
     * @param username    登入帳號
     * @param rawPassword 明文密碼（僅用於雜湊，不落庫）
     * @param displayName 顯示名稱（通常為 owner 姓名）
     * @param role        角色（一般為 ROLE_USER）
     */
    void createUser(String username, String rawPassword, String displayName, String role);

    User findByUsername(String username);

    List<User> getAllUsers();

    /**
     * 使用者自助改密碼：須提供正確舊密碼，新密碼最少 8 字元。
     */
    void changePassword(String username, String oldRawPassword, String newRawPassword);

    /**
     * 中央變更 owner 顯示名稱（item3）：改 User.displayName，該使用者名下所有帳戶皆同步反映。
     * 僅供 admin 使用。
     */
    void updateDisplayName(String username, String displayName);

    /**
     * admin 重設使用者密碼（item4）：無需舊密碼，新密碼最少 8 字元、BCrypt 雜湊。
     */
    void adminSetPassword(String username, String newRawPassword);
}
