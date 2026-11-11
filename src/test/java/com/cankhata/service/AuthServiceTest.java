package com.cankhata.service;

import com.cankhata.dao.AuditDao;
import com.cankhata.dao.UserDao;
import com.cankhata.exception.AuthException;
import com.cankhata.exception.BusinessRuleException;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.exception.ValidationException;
import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.model.User;
import com.cankhata.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Service rules tested with a fake DAO (an in-memory list), so no database is needed. */
class AuthServiceTest {

    /** Fake DAO: same methods as UserDao, but stores users in a list. */
    static class FakeUserDao extends UserDao {
        final List<User> store = new ArrayList<>();

        @Override
        public Optional<User> findByLogin(String login) {
            return store.stream().filter(u -> login.equals(u.getPhone()) || login.equals(u.getEmail())).findFirst();
        }

        @Override
        public boolean existsByPhoneOrEmail(String phone, String email) {
            return store.stream().anyMatch(u -> phone.equals(u.getPhone()) || (email != null && email.equals(u.getEmail())));
        }

        @Override
        public long insert(User u) {
            u.setId(store.size() + 1);
            store.add(u);
            return u.getId();
        }

        @Override
        public Optional<User> findById(long id) {
            return store.stream().filter(u -> u.getId() == id).findFirst();
        }

        @Override
        public void updatePassword(long id, String passwordHash) {
            findById(id).ifPresent(u -> u.setPasswordHash(passwordHash));
        }

        @Override
        public long countAll() {
            return store.size();
        }
    }

    FakeUserDao dao;
    AuthService auth;
    SessionUser owner;

    @BeforeEach
    void setUp() {
        dao = new FakeUserDao();
        AuditService audit = new AuditService(new AuditDao() {
            @Override
            public void insert(Long userId, String action, String entity, Long entityId, String detail, String ip) {
                // do nothing: no database in unit tests
            }
        });
        auth = new AuthService(dao, audit);

        User o = new User();
        o.setRole(Role.OWNER);
        o.setName("Boss");
        o.setPhone("9876543210");
        o.setPasswordHash(PasswordUtil.hash("secret123"));
        o.setActive(true);
        dao.insert(o);
        owner = new SessionUser(o.getId(), o.getName(), Role.OWNER);
    }

    @Test
    void correctPasswordLogsIn() {
        SessionUser u = auth.login("+91 98765 43210", "secret123", "1.1.1.1");
        assertEquals(Role.OWNER, u.getRole());
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameMessage() {
        String m1 = assertThrows(AuthException.class, () -> auth.login("9876543210", "wrongpass1", "ip")).getMessage();
        String m2 = assertThrows(AuthException.class, () -> auth.login("9123456789", "wrongpass1", "ip")).getMessage();
        assertEquals(m1, m2);
    }

    @Test
    void fiveWrongAttemptsLockTheAccountEvenForTheRightPassword() {
        for (int i = 0; i < 5; i++) {
            assertThrows(AuthException.class, () -> auth.login("9876543210", "wrong-pass-1", "ip"));
        }
        AuthException e = assertThrows(AuthException.class, () -> auth.login("9876543210", "secret123", "ip"));
        assertTrue(e.getMessage().contains("Too many"));
    }

    @Test
    void inactiveUserCannotLogIn() {
        dao.store.get(0).setActive(false);
        assertThrows(AuthException.class, () -> auth.login("9876543210", "secret123", "ip"));
    }

    @Test
    void ownerCanCreateDeliveryBoy() {
        long id = auth.createUser(owner, "Muthu", "9000000001", "", "pass1234", "DELIVERY_BOY", "ip");
        assertTrue(id > 1);
        assertEquals(Role.DELIVERY_BOY, dao.store.get(1).getRole());
        assertNotEquals("pass1234", dao.store.get(1).getPasswordHash()); // stored hashed, never plain
    }

    @Test
    void nonOwnerCannotCreateUsers() {
        SessionUser boy = new SessionUser(2, "Muthu", Role.DELIVERY_BOY);
        assertThrows(ForbiddenException.class, () -> auth.createUser(boy, "X Y", "9000000002", null, "pass1234", "DELIVERY_BOY", "ip"));
    }

    @Test
    void duplicatePhoneIsRejected() {
        assertThrows(BusinessRuleException.class, () -> auth.createUser(owner, "Dup", "9876543210", null, "pass1234", "DELIVERY_BOY", "ip"));
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(ValidationException.class, () -> auth.createUser(owner, "A", "9000000003", null, "pass1234", "DELIVERY_BOY", "ip"));
        assertThrows(ValidationException.class, () -> auth.createUser(owner, "Good Name", "123", null, "pass1234", "DELIVERY_BOY", "ip"));
        assertThrows(ValidationException.class, () -> auth.createUser(owner, "Good Name", "9000000003", null, "weak", "DELIVERY_BOY", "ip"));
        assertThrows(ValidationException.class, () -> auth.createUser(owner, "Good Name", "9000000003", null, "pass1234", "CUSTOMER", "ip"));
        assertThrows(ValidationException.class, () -> auth.createUser(owner, "Good Name", "9000000003", null, "pass1234", "HACKER", "ip"));
    }

    @Test
    void ownerCanResetAnotherUsersPassword() {
        long id = auth.createUser(owner, "Muthu", "9000000001", "", "pass1234", "DELIVERY_BOY", "ip");
        auth.resetPassword(owner, id, "newpass99", "ip");
        assertDoesNotThrow(() -> auth.login("9000000001", "newpass99", "ip"));
        assertThrows(AuthException.class, () -> auth.login("9000000001", "pass1234", "ip"));
    }

    @Test
    void resetLiftsALockout() {
        long id = auth.createUser(owner, "Muthu", "9000000001", "", "pass1234", "DELIVERY_BOY", "ip");
        for (int i = 0; i < 5; i++) assertThrows(AuthException.class, () -> auth.login("9000000001", "wrong-pass-1", "ip"));
        auth.resetPassword(owner, id, "newpass99", "ip");
        assertDoesNotThrow(() -> auth.login("9000000001", "newpass99", "ip"));
    }

    @Test
    void onlyOwnerCanReset() {
        SessionUser boy = new SessionUser(2, "Muthu", Role.DELIVERY_BOY);
        assertThrows(ForbiddenException.class, () -> auth.resetPassword(boy, 1, "newpass99", "ip"));
    }

    @Test
    void resetRejectsWeakPassword() {
        assertThrows(ValidationException.class, () -> auth.resetPassword(owner, 1, "weak", "ip"));
    }

    @Test
    void changeOwnPasswordNeedsTheOldOne() {
        assertThrows(ValidationException.class, () -> auth.changeOwnPassword(owner, "wrong-old1", "newpass99", "newpass99", "ip"));
        auth.changeOwnPassword(owner, "secret123", "newpass99", "newpass99", "ip");
        assertDoesNotThrow(() -> auth.login("9876543210", "newpass99", "ip"));
        assertThrows(AuthException.class, () -> auth.login("9876543210", "secret123", "ip"));
    }

    @Test
    void changeOwnPasswordChecksConfirmationAndStrengthAndDifference() {
        assertThrows(ValidationException.class, () -> auth.changeOwnPassword(owner, "secret123", "newpass99", "different9", "ip"));
        assertThrows(ValidationException.class, () -> auth.changeOwnPassword(owner, "secret123", "weak", "weak", "ip"));
        assertThrows(ValidationException.class, () -> auth.changeOwnPassword(owner, "secret123", "secret123", "secret123", "ip"));
    }
}
