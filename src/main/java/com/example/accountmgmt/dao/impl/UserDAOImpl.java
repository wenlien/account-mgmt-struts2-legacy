package com.example.accountmgmt.dao.impl;

import com.example.accountmgmt.dao.UserDAO;
import com.example.accountmgmt.hibernate.model.User;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 使用者 DAO 實作（Hibernate 4 SessionFactory；遷移雷同其他 DAO）。
 */
public class UserDAOImpl implements UserDAO {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public User findByUsername(String username) {
        return (User) sessionFactory.getCurrentSession().get(User.class, username);
    }

    @Override
    @Transactional
    public void addUser(User user) {
        sessionFactory.getCurrentSession().save(user);
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        sessionFactory.getCurrentSession().update(user);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<User> getAllUsers() {
        return (List<User>) sessionFactory.getCurrentSession()
                .createQuery("from User u order by u.username").list();
    }
}
