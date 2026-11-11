package com.cankhata.service;

import com.cankhata.dao.UserDao;
import com.cankhata.exception.AuthException;
import com.cankhata.exception.ForbiddenException;
import com.cankhata.exception.ValidationException;
import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import com.cankhata.model.User;
import com.cankhata.util.Config;
import com.cankhata.util.PasswordUtil;
import com.cankhata.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Login rules and user creation. Business logic lives HERE, not in servlets.
 * Because it only needs a UserDao, a test can pass a fake DAO and check the rules without a database.
 */
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    static final int MAX_FAILS = 5;
    static final long LOCK_MS = 10 * 60_000L;

    private static class Attempts {
        int fails;
        long lockedUntil;
    }

    // Per server instance. With several instances an attacker gets 5 tries per instance; fine for this project.
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final UserDao users;
    private final AuditService audit;

    public AuthService(UserDao users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    /** Returns who logged in, or throws AuthException. The same message is used for every failure (no hints for attackers). */
    public SessionUser login(String login, String password, String ip) {
        String key = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("Enter your phone or email and password");
        }
        checkNotLocked(key);

        // Normalise a phone number typed as +91 98765 43210
        String lookup = key.contains("@") ? key : tryPhone(key);
        Optional<User> found = users.findByLogin(lookup);

        // Always run BCrypt, even for unknown users, so response time does not reveal which logins exist.
        boolean passwordOk = PasswordUtil.verify(password, found.map(User::getPasswordHash).orElse(PasswordUtil.DUMMY_HASH));

        if (found.isEmpty() || !passwordOk || !found.get().isActive()) {
            registerFailure(key, ip);
            throw new AuthException("Wrong phone/email or password");
        }
        attempts.remove(key);
        User u = found.get();
        audit.log(u.getId(), "LOGIN", "user", u.getId(), null, ip);
        return new SessionUser(u.getId(), u.getName(), u.getRole());
    }

    /** Only an OWNER may create staff accounts. Customers get their own screen in release 2. */
    public long createUser(SessionUser actor, String name, String phone, String email, String password, String roleName, String ip) {
        if (actor == null || actor.getRole() != Role.OWNER) throw new ForbiddenException("Only the owner can create users");

        String cleanName = Validator.name(name);
        String cleanPhone = Validator.phone(phone);
        String cleanEmail = Validator.optionalEmail(email);
        Validator.password(password);
        Role role;
        try {
            role = Role.valueOf(roleName == null ? "" : roleName);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Choose a role");
        }
        if (role == Role.CUSTOMER) throw new ValidationException("Customers are added from the Customers screen");

        if (users.existsByPhoneOrEmail(cleanPhone, cleanEmail)) {
            throw new com.cankhata.exception.BusinessRuleException("This phone number or email is already registered");
        }
        User u = new User();
        u.setRole(role);
        u.setName(cleanName);
        u.setPhone(cleanPhone);
        u.setEmail(cleanEmail);
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setActive(true);
        long id = users.insert(u);
        audit.log(actor.getId(), "USER_CREATED", "user", id, role.name() + " " + cleanPhone, ip);
        return id;
    }

    /** Owner sets a new password for any staff member or customer login (they forgot theirs). Also lifts a login lockout. */
    public void resetPassword(SessionUser actor, long userId, String newPassword, String ip) {
        requireOwner(actor);
        Validator.password(newPassword);
        User target = users.findById(userId).orElseThrow(() -> new com.cankhata.exception.NotFoundException("User not found"));
        users.updatePassword(userId, PasswordUtil.hash(newPassword));
        if (target.getPhone() != null) attempts.remove(target.getPhone().toLowerCase(Locale.ROOT));
        if (target.getEmail() != null) attempts.remove(target.getEmail().toLowerCase(Locale.ROOT));
        audit.log(actor.getId(), "PASSWORD_RESET_BY_OWNER", "user", userId, null, ip);
    }

    /** Any logged-in user changes their OWN password. The old password must be right (a stolen open session alone is not enough). */
    public void changeOwnPassword(SessionUser actor, String oldPassword, String newPassword, String confirm, String ip) {
        if (actor == null) throw new AuthException("Please log in");
        if (oldPassword == null || oldPassword.isEmpty()) throw new ValidationException("Enter your current password");
        if (newPassword == null || !newPassword.equals(confirm)) throw new ValidationException("The new passwords do not match");
        Validator.password(newPassword);
        User me = users.findById(actor.getId()).orElseThrow(() -> new AuthException("Please log in"));
        if (!PasswordUtil.verify(oldPassword, me.getPasswordHash())) {
            audit.log(actor.getId(), "PASSWORD_CHANGE_FAILED", "user", actor.getId(), null, ip);
            throw new ValidationException("Your current password is wrong");
        }
        if (oldPassword.equals(newPassword)) throw new ValidationException("Choose a password different from the current one");
        users.updatePassword(actor.getId(), PasswordUtil.hash(newPassword));
        audit.log(actor.getId(), "PASSWORD_CHANGED", "user", actor.getId(), null, ip);
    }

    /** First start only: if there are no users, create the owner from OWNER_PHONE / OWNER_PASSWORD (environment variables). */
    public void bootstrapOwner() {
        if (users.countAll() > 0) return;
        String phone = Config.get("OWNER_PHONE", null);
        String password = Config.get("OWNER_PASSWORD", null);
        if (phone == null || password == null) {
            log.warn("No users exist and OWNER_PHONE / OWNER_PASSWORD are not set, so nobody can log in yet");
            return;
        }
        User u = new User();
        u.setRole(Role.OWNER);
        u.setName(Config.get("OWNER_NAME", "Owner"));
        u.setPhone(Validator.phone(phone));
        u.setEmail(Validator.optionalEmail(Config.get("OWNER_EMAIL", null)));
        u.setPasswordHash(PasswordUtil.hash(Validator.password(password)));
        u.setActive(true);
        long id = users.insert(u);
        audit.log(id, "OWNER_BOOTSTRAPPED", "user", id, null, "startup");
        log.info("First owner account created (id {})", id);
    }

    public List<User> listUsers(SessionUser actor, String q, int page, int size) {
        requireOwner(actor);
        return users.search(q, size, (page - 1) * size);
    }

    public List<User> listDeliveryBoys(SessionUser actor) {
        requireOwner(actor);
        return users.listActiveDeliveryBoys();
    }

    public long countUsers(SessionUser actor, String q) {
        requireOwner(actor);
        return users.count(q);
    }

    private static void requireOwner(SessionUser actor) {
        if (actor == null || actor.getRole() != Role.OWNER) throw new ForbiddenException("Only the owner can do this");
    }

    private static String tryPhone(String key) {
        try {
            return Validator.phone(key);
        } catch (ValidationException e) {
            return key; // not a phone; will simply not be found
        }
    }

    private void checkNotLocked(String key) {
        Attempts a = attempts.get(key);
        if (a != null && a.lockedUntil > System.currentTimeMillis()) {
            long minutes = Math.max(1, (a.lockedUntil - System.currentTimeMillis()) / 60_000);
            throw new AuthException("Too many wrong attempts. Try again in " + minutes + " minute(s).");
        }
    }

    private void registerFailure(String key, String ip) {
        Attempts a = attempts.computeIfAbsent(key, k -> new Attempts());
        synchronized (a) {
            if (a.lockedUntil != 0 && a.lockedUntil <= System.currentTimeMillis()) { // old lock expired
                a.fails = 0;
                a.lockedUntil = 0;
            }
            a.fails++;
            if (a.fails >= MAX_FAILS) {
                a.lockedUntil = System.currentTimeMillis() + LOCK_MS;
                log.warn("Login locked for '{}' from {} after {} failures", key, ip, a.fails);
                audit.log(null, "LOGIN_LOCKED", "user", null, "login=" + key, ip);
            }
        }
    }
}
