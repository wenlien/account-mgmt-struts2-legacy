package com.example.accountmgmt.action;

import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.hibernate.model.AuditLog;
import com.example.accountmgmt.hibernate.model.User;
import com.example.accountmgmt.security.SecurityUtil;
import com.example.accountmgmt.service.AccountService;
import com.example.accountmgmt.service.AuditService;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankValidationException;
import com.example.accountmgmt.service.CodedException;
import com.example.accountmgmt.service.UserService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 使用者相關 Action。
 *
 * <ul>
 *   <li>使用者自助（#8）：變更自己的密碼（新密碼需輸入兩次，#2）。</li>
 *   <li>admin 中央管理（item3/4/6，URL 由 SecurityConfig 限 ADMIN）：
 *       Edit Owner 一次改 owner 顯示名稱 + 電話 + 地址（套用該 user 名下所有帳戶）+ 選填新密碼（兩次）；
 *       另有獨立 Reset Password（兩次）。</li>
 * </ul>
 */
public class UserAction extends ActionSupport {

    // 自助改密碼
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;

    // admin 操作目標與可編輯欄位
    private String targetUsername;
    private String displayName;
    private String phone;
    private String address;

    /** Edit Owner 頁面就地呈現：該使用者的 profile 變更歷史（owner name / 密碼 / 電話 / 地址）。 */
    private List<AuditLog> profileChanges;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuditService auditService;

    // ---- 使用者自助改密碼（#2：新密碼兩次）----

    public String changePasswordForm() {
        return INPUT;
    }

    public String changePassword() {
        String actor = SecurityUtil.currentUsername();
        try {
            if (newPassword == null || !newPassword.equals(confirmNewPassword)) {
                throw new BankValidationException(BankErrorCode.PASSWORD_MISMATCH,
                        "New password and confirmation do not match");
            }
            userService.changePassword(actor, oldPassword, newPassword);
            auditService.logSuccess(actor, "CHANGE_PASSWORD", null, "user=" + actor + " changed: password (self)");
            addActionMessage("Password changed successfully.");
            return SUCCESS;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "CHANGE_PASSWORD", null, CodedException.codeOf(ex),
                    "user=" + actor + " password change failed (self): " + ex.getMessage());
            addActionError(ex.getMessage());
            return INPUT;
        }
    }

    // ---- admin：Edit Owner（改名 + 電話 + 地址 + 選填密碼，item3/6）----

    public String editOwnerForm() {
        User u = userService.findByUsername(targetUsername);
        if (u == null) {
            addActionError("User not found: " + targetUsername);
            return ERROR;
        }
        displayName = u.getDisplayName();
        // 預填目前電話 / 地址（取該 user 任一帳戶；#5 同 user 各帳戶電話一致）
        List<Account> accts = accountService.getAccountsByOwner(targetUsername);
        if (accts != null && !accts.isEmpty()) {
            phone = accts.get(0).getPhone();
            address = accts.get(0).getAddress();
        }
        // item：就地載入該使用者的 profile 變更歷史（owner name / 密碼 / 電話 / 地址）。
        profileChanges = auditService.profileChangeHistory(targetUsername);
        return INPUT;
    }

    public List<AuditLog> getProfileChanges() {
        return profileChanges;
    }

    public String updateOwner() {
        String actor = SecurityUtil.currentUsername();
        boolean changingPw = newPassword != null && !newPassword.isEmpty();
        // 先比對舊值，算出「實際打算變更哪些欄位」（供成功/失敗都寫進 detail；只列有變更的欄位）。
        String changedFields = describeOwnerChanges(changingPw);
        try {
            // 選填改密碼時，兩次需一致
            if (changingPw && !newPassword.equals(confirmNewPassword)) {
                throw new BankValidationException(BankErrorCode.PASSWORD_MISMATCH,
                        "New password and confirmation do not match");
            }
            userService.updateDisplayName(targetUsername, displayName);
            accountService.updateContactForOwner(targetUsername, phone, address);
            if (changingPw) {
                userService.adminSetPassword(targetUsername, newPassword);
            }
            auditService.logSuccess(actor, "UPDATE_OWNER", null,
                    "user=" + targetUsername + " changed: " + changedFields);
            return SUCCESS;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "UPDATE_OWNER", null, CodedException.codeOf(ex),
                    "user=" + targetUsername + " change failed (attempted: " + changedFields + "): " + ex.getMessage());
            addActionError(ex.getMessage());
            profileChanges = auditService.profileChangeHistory(targetUsername);
            return INPUT;
        }
    }

    /**
     * 比對舊值，列出打算變更的欄位（owner name / phone / address / password）。
     * 只列出實際有變更的欄位；都沒變則回 "(no field change)"。
     */
    private String describeOwnerChanges(boolean changingPw) {
        User u = userService.findByUsername(targetUsername);
        String oldName = (u != null) ? u.getDisplayName() : null;
        String oldPhone = null;
        String oldAddress = null;
        List<Account> accts = accountService.getAccountsByOwner(targetUsername);
        if (accts != null && !accts.isEmpty()) {
            oldPhone = accts.get(0).getPhone();
            oldAddress = accts.get(0).getAddress();
        }
        StringBuilder sb = new StringBuilder();
        appendIfChanged(sb, "owner name", oldName, displayName);
        appendIfChanged(sb, "phone", oldPhone, phone);
        appendIfChanged(sb, "address", oldAddress, address);
        if (changingPw) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("password");
        }
        return sb.length() == 0 ? "(no field change)" : sb.toString();
    }

    private void appendIfChanged(StringBuilder sb, String field, String oldVal, String newVal) {
        String o = (oldVal == null) ? "" : oldVal.trim();
        String n = (newVal == null) ? "" : newVal.trim();
        if (!o.equals(n)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(field);
        }
    }

    // ---- admin：獨立重設使用者密碼（#2：兩次；item4）----

    public String resetPasswordForm() {
        User u = userService.findByUsername(targetUsername);
        if (u == null) {
            addActionError("User not found: " + targetUsername);
            return ERROR;
        }
        return INPUT;
    }

    public String resetPassword() {
        String actor = SecurityUtil.currentUsername();
        try {
            if (newPassword == null || !newPassword.equals(confirmNewPassword)) {
                throw new BankValidationException(BankErrorCode.PASSWORD_MISMATCH,
                        "New password and confirmation do not match");
            }
            userService.adminSetPassword(targetUsername, newPassword);
            auditService.logSuccess(actor, "CHANGE_PASSWORD", null,
                    "user=" + targetUsername + " changed: password (admin reset)");
            return SUCCESS;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            auditService.logFailure(actor, "CHANGE_PASSWORD", null, CodedException.codeOf(ex),
                    "user=" + targetUsername + " password reset failed (admin): " + ex.getMessage());
            addActionError(ex.getMessage());
            return INPUT;
        }
    }

    // Getters and setters

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
