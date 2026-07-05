package com.example.accountmgmt.security;

import com.example.accountmgmt.hibernate.model.User;
import com.example.accountmgmt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * DB-backed UserDetailsService（#6）：從 users 表載入客戶帳號供 Spring Security 認證。
 *
 * <p>admin 走 in-memory（SecurityConfig），不經此。密碼比對用 BCrypt（見 SecurityConfig
 * 的 passwordEncoder 設定）。</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userService.findByUsername(username);
        if (u == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        String role = (u.getRole() == null || u.getRole().trim().isEmpty()) ? "ROLE_USER" : u.getRole().trim();
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }
}
