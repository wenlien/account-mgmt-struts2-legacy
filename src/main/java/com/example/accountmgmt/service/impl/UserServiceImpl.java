package com.example.accountmgmt.service.impl;

import com.example.accountmgmt.dao.UserDAO;
import com.example.accountmgmt.hibernate.model.User;
import com.example.accountmgmt.service.BankErrorCode;
import com.example.accountmgmt.service.BankValidationException;
import com.example.accountmgmt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public class UserServiceImpl implements UserService {

    /** 密碼最短長度（#6 安全預設）。 */
    static final int MIN_PASSWORD_LENGTH = 8;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void createUser(String username, String rawPassword, String displayName, String role) {
        String u = username == null ? null : username.trim();
        if (u == null || u.isEmpty()) {
            throw new BankValidationException(BankErrorCode.USERNAME_REQUIRED, "Username is required");
        }
        if (userDAO.findByUsername(u) != null) {
            throw new BankValidationException(BankErrorCode.USERNAME_ALREADY_EXISTS,
                    "Username already exists: " + u);
        }
        requireValidPassword(rawPassword);
        String hash = passwordEncoder.encode(rawPassword);
        String r = (role == null || role.trim().isEmpty()) ? "ROLE_USER" : role.trim();
        userDAO.addUser(new User(u, hash, r, displayName, new Date()));
    }

    @Override
    @Transactional
    public User findByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    @Override
    @Transactional
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldRawPassword, String newRawPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new BankValidationException(BankErrorCode.USER_NOT_FOUND, "User not found: " + username);
        }
        if (oldRawPassword == null || !passwordEncoder.matches(oldRawPassword, user.getPasswordHash())) {
            throw new BankValidationException(BankErrorCode.OLD_PASSWORD_MISMATCH,
                    "Current password is incorrect");
        }
        requireValidPassword(newRawPassword);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userDAO.updateUser(user);
    }

    @Override
    @Transactional
    public void updateDisplayName(String username, String displayName) {
        User user = requireUser(username);
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new BankValidationException(BankErrorCode.OWNER_NAME_REQUIRED, "Owner name is required");
        }
        user.setDisplayName(displayName.trim());
        userDAO.updateUser(user);
    }

    @Override
    @Transactional
    public void adminSetPassword(String username, String newRawPassword) {
        User user = requireUser(username);
        requireValidPassword(newRawPassword);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userDAO.updateUser(user);
    }

    private User requireUser(String username) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new BankValidationException(BankErrorCode.USER_NOT_FOUND, "User not found: " + username);
        }
        return user;
    }

    private void requireValidPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BankValidationException(BankErrorCode.PASSWORD_TOO_SHORT,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
