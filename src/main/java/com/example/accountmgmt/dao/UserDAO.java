package com.example.accountmgmt.dao;

import com.example.accountmgmt.hibernate.model.User;

import java.util.List;

public interface UserDAO {
    User findByUsername(String username);
    void addUser(User user);
    void updateUser(User user);
    List<User> getAllUsers();
}
