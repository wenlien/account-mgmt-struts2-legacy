package com.example.accountmgmt.service.impl;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.hibernate.model.Account;
import com.example.accountmgmt.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDAO accountDAO;

    /** 供測試 / XML setter 注入用。 */
    public void setAccountDAO(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @Override
    public List<Account> getAllAccounts() {
        return accountDAO.getAllAccounts();
    }

    @Override
    public Account getAccountByNo(String accountNo) {
        return accountDAO.getAccountByNo(accountNo);
    }

    @Override
    public void addAccount(Account account) {
        accountDAO.addAccount(account);
    }

    @Override
    public void updateAccount(Account account) {
        accountDAO.updateAccount(account);
    }

    @Override
    public void deleteAccount(String accountNo) {
        accountDAO.deleteAccount(accountNo);
    }
}
