package com.example.accountmgmt.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 從 Spring Security context 讀取當前登入者資訊的工具（#7/#8/#9）。
 *
 * <p>Action 層用它取得 actor（稽核）、判斷 admin/user（授權過濾）。</p>
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 當前登入者 username；未登入回 null。 */
    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        return "anonymousUser".equals(name) ? null : name;
    }

    /** 當前登入者是否為 ADMIN。 */
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
