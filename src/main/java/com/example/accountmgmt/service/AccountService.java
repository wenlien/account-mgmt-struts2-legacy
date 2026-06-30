package com.example.accountmgmt.service;

import com.example.accountmgmt.hibernate.model.Account;

import java.util.List;

public interface AccountService {
    List<Account> getAllAccounts();
    Account getAccountByNo(String accountNo);
    void addAccount(Account account);
    void updateAccount(Account account);
    void deleteAccount(String accountNo);
}
