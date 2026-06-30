package com.example.accountmgmt.action;

import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.service.AccountService;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 帳戶 CRUD 的 Struts2 Action（re-skin 自原 BookAction）。
 *
 * 透過 struts2-spring-plugin 由 Spring 注入 service（遷移雷：
 * Spring Boot 3 改用 @RestController + 建構子注入）。
 */
public class AccountAction extends ActionSupport {

    private Account account;
    private List<Account> accountList;
    private String accountNo;

    @Autowired
    private AccountService accountService;

    public String list() {
        accountList = accountService.getAllAccounts();
        return SUCCESS;
    }

    public String add() {
        return INPUT;
    }

    public String save() {
        accountService.addAccount(account);
        return SUCCESS;
    }

    public String edit() {
        account = accountService.getAccountByNo(accountNo);
        if (account == null) {
            return ERROR;
        }
        return INPUT;
    }

    public String update() {
        accountService.updateAccount(account);
        return SUCCESS;
    }

    public String delete() {
        accountService.deleteAccount(accountNo);
        return SUCCESS;
    }

    // Getters and setters

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
}
