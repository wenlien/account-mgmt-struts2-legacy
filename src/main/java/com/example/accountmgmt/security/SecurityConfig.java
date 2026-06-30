package com.example.accountmgmt.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

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
        // Spring Security 4.x 風格的 in-memory 認證（明文比對）。
        auth.inMemoryAuthentication()
                .withUser(adminUsername())
                .password(adminPassword())
                .roles("USER");
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 靜態資源放行。
        web.ignoring().antMatchers("/css/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/login", "/login.jsp").permitAll()
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
