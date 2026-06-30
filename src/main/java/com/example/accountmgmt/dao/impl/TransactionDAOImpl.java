package com.example.accountmgmt.dao.impl;

import com.example.accountmgmt.dao.TransactionDAO;
import com.example.accountmgmt.hibernate.model.Transaction;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 交易 DAO 實作。
 *
 * 用 Hibernate HQL + {@code SessionFactory.getCurrentSession()}（遷移雷）。
 */
public class TransactionDAOImpl implements TransactionDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactionsByAccount(String accountNo) {
        return (List<Transaction>) sessionFactory.getCurrentSession()
                .createQuery("from Transaction t where t.account.accountNo = :accountNo "
                        + "order by t.createdAt desc")
                .setParameter("accountNo", accountNo)
                .list();
    }

    @Override
    @Transactional
    public void addTransaction(Transaction transaction) {
        sessionFactory.getCurrentSession().save(transaction);
    }
}
