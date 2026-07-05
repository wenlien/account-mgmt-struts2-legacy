package com.example.accountmgmt.dao.impl;

import com.example.accountmgmt.dao.AccountDAO;
import com.example.accountmgmt.hibernate.model.Account;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 帳戶 DAO 實作。
 *
 * 刻意保留 Hibernate {@code SessionFactory.getCurrentSession()} 寫法（遷移雷：
 * Spring Boot 3 / Spring Data JPA 要換成 Repository）。對應 design.md §3.3。
 */
public class AccountDAOImpl implements AccountDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Account> getAllAccounts() {
        return (List<Account>) sessionFactory.getCurrentSession()
                .createCriteria(Account.class).list();
    }

    @Override
    @Transactional
    public Account getAccountByNo(String accountNo) {
        return (Account) sessionFactory.getCurrentSession().get(Account.class, accountNo);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Account> getAccountsByOwnerUsername(String ownerUsername) {
        return (List<Account>) sessionFactory.getCurrentSession()
                .createQuery("from Account a where a.ownerUsername = :owner order by a.accountNo")
                .setParameter("owner", ownerUsername)
                .list();
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Account> getAccountsByPhone(String phone) {
        return (List<Account>) sessionFactory.getCurrentSession()
                .createQuery("from Account a where a.phone = :phone order by a.accountNo")
                .setParameter("phone", phone)
                .list();
    }

    @Override
    @Transactional
    public void addAccount(Account account) {
        sessionFactory.getCurrentSession().save(account);
    }

    @Override
    @Transactional
    public void updateAccount(Account account) {
        sessionFactory.getCurrentSession().update(account);
    }

    @Override
    @Transactional
    public int getMaxSequence(char prefix) {
        String hql = "SELECT MAX(CAST(SUBSTRING(a.accountNo, 2) AS integer)) FROM Account a WHERE a.accountNo LIKE :pattern";
        Object result = sessionFactory.getCurrentSession()
                .createQuery(hql)
                .setParameter("pattern", prefix + "%")
                .uniqueResult();
        return result == null ? 0 : ((Number) result).intValue();
    }
}
