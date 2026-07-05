package com.example.accountmgmt.service;

import com.example.accountmgmt.dao.UserDAO;
import com.example.accountmgmt.hibernate.model.User;
import com.example.accountmgmt.service.impl.UserServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * UserService 的 JUnit 4 測試（真 BCryptPasswordEncoder + in-memory fake UserDAO）。
 * 涵蓋 createUser（雜湊/唯一/長度）、changePassword、updateDisplayName、adminSetPassword。
 */
public class UserServiceTest {

    private UserServiceImpl service;
    private FakeUserDAO userDAO;
    private BCryptPasswordEncoder encoder;

    @Before
    public void setUp() {
        userDAO = new FakeUserDAO();
        encoder = new BCryptPasswordEncoder();
        service = new UserServiceImpl();
        service.setUserDAO(userDAO);
        service.setPasswordEncoder(encoder);
    }

    // ---- createUser ----

    @Test
    public void createUser_success_hashesPassword() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        User u = service.findByUsername("alice");
        assertNotNull(u);
        assertEquals("Alice", u.getDisplayName());
        assertEquals("ROLE_USER", u.getRole());
        assertFalse("password must not be stored in plain text", "password1".equals(u.getPasswordHash()));
        assertTrue(encoder.matches("password1", u.getPasswordHash()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUser_duplicateUsername_throws() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.createUser("alice", "password2", "Alice2", "ROLE_USER");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUser_shortPassword_throws() {
        service.createUser("bob", "short", "Bob", "ROLE_USER"); // <8
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUser_missingUsername_throws() {
        service.createUser("  ", "password1", "X", "ROLE_USER");
    }

    // ---- changePassword ----

    @Test
    public void changePassword_success() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.changePassword("alice", "password1", "newpass12");
        assertTrue(encoder.matches("newpass12", service.findByUsername("alice").getPasswordHash()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void changePassword_wrongOld_throws() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.changePassword("alice", "wrongold", "newpass12");
    }

    @Test(expected = IllegalArgumentException.class)
    public void changePassword_shortNew_throws() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.changePassword("alice", "password1", "short");
    }

    // ---- updateDisplayName ----

    @Test
    public void updateDisplayName_success() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.updateDisplayName("alice", "Alice C.");
        assertEquals("Alice C.", service.findByUsername("alice").getDisplayName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateDisplayName_empty_throws() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.updateDisplayName("alice", "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateDisplayName_userNotFound_throws() {
        service.updateDisplayName("ghost", "X");
    }

    // ---- adminSetPassword ----

    @Test
    public void adminSetPassword_success_noOldNeeded() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.adminSetPassword("alice", "resetpw12");
        assertTrue(encoder.matches("resetpw12", service.findByUsername("alice").getPasswordHash()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void adminSetPassword_short_throws() {
        service.createUser("alice", "password1", "Alice", "ROLE_USER");
        service.adminSetPassword("alice", "short");
    }

    @Test(expected = IllegalArgumentException.class)
    public void adminSetPassword_userNotFound_throws() {
        service.adminSetPassword("ghost", "resetpw12");
    }

    // ---- in-memory fake ----

    static class FakeUserDAO implements UserDAO {
        private final Map<String, User> store = new HashMap<String, User>();

        public User findByUsername(String username) {
            return store.get(username);
        }

        public void addUser(User user) {
            store.put(user.getUsername(), user);
        }

        public void updateUser(User user) {
            store.put(user.getUsername(), user);
        }

        public List<User> getAllUsers() {
            return new ArrayList<User>(store.values());
        }
    }
}
