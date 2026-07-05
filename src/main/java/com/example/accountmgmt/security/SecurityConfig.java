package com.example.accountmgmt.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security 設定（登入認證）。
 *
 * <p><b>遷移雷（design.md §3.3 / §6 / §11）</b>：本類別繼承
 * {@link WebSecurityConfigurerAdapter}，此基底類別在 Spring Security 6
 * 已被移除，遷移到 Spring Boot 3 時必須改寫成宣告 {@code SecurityFilterChain}
 * bean 的形式。這是課程刻意保留的高價值遷移點。</p>
 *
 * <p>登入帳密走環境變數（不硬編密鑰，對應 S2）：{@code ADMIN_USERNAME} /
 * {@code ADMIN_PASSWORD}，由 Phase 2 的 Docker Compose / .env 注入。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /** DB 客戶帳號的 UserDetailsService（#6）。 */
    @Autowired
    private CustomUserDetailsService userDetailsService;

    /** BCrypt 密碼雜湊器（spring-config.xml 定義）。 */
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminUsername() {
        String u = System.getenv("ADMIN_USERNAME");
        return (u == null || u.isEmpty()) ? "admin" : u;
    }

    private String adminPassword() {
        // 不提供預設值：未注入 ADMIN_PASSWORD 時登入功能不可用（避免硬編密碼）。
        String p = System.getenv("ADMIN_PASSWORD");
        return (p == null) ? "" : p;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // (1) admin：in-memory、明文比對、ROLE_ADMIN（帳密走環境變數，不硬編）。
        auth.inMemoryAuthentication()
                .withUser(adminUsername())
                .password(adminPassword())
                .roles("ADMIN");
        // (2) 一般客戶：DB-backed UserDetailsService + BCrypt（#6）。
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 靜態資源放行。
        web.ignoring().antMatchers("/css/**", "/images/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/login", "/login.jsp").permitAll()
                // ---- ADMIN 專屬（#7/#8）：開戶、帳戶生命週期、櫃檯存提款、稽核頁 ----
                // 用 /name* 一併涵蓋 /name 與 /name.action 兩種 Struts2 URL 形式。
                .antMatchers(
                        "/openAccount*", "/addAccount*", "/saveAccount*",
                        "/freezeAccount*", "/activateAccount*",
                        "/closeAccountForm*", "/closeAccount*", "/batchCloseForm*", "/closeAccounts*",
                        "/freezeSelected*", "/activateSelected*",
                        "/editOwnerForm*", "/updateOwner*", "/resetPasswordForm*", "/resetPassword*",
                        "/auditLog*").hasRole("ADMIN")
                // ---- 已登入即可（user + admin）：清單(依角色過濾)、交易查詢、存/提/轉(限自己帳戶,Action 層 ownership 檢查)、改密碼 ----
                .antMatchers(
                        "/accountList*", "/transactions*",
                        "/deposit*", "/withdraw*", "/transfer*",
                        "/changePasswordForm*", "/changePassword*").authenticated()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login.jsp")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/accountList", true)
                .failureUrl("/login.jsp?error=1")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
                .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.jsp?logout=1")
                .permitAll()
                .and()
            // 簡化起點 app：關閉 CSRF（遷移後現代化階段再評估開啟）。
            .csrf().disable();
    }
}
