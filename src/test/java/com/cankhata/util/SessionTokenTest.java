package com.cankhata.util;

import com.cankhata.model.Role;
import com.cankhata.model.SessionUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SessionTokenTest {

    @BeforeAll
    static void setUp() {
        SessionToken.init("unit-test-secret-unit-test-secret-1234");
    }

    private static SessionUser ravi() {
        return new SessionUser(7, "Ravi|Kumar", Role.DELIVERY_BOY); // a '|' in the name must not break parsing
    }

    @Test
    void validTokenRoundTrips() {
        Optional<SessionUser> u = SessionToken.parse(SessionToken.create(ravi()));
        assertTrue(u.isPresent());
        assertEquals(7, u.get().getId());
        assertEquals(Role.DELIVERY_BOY, u.get().getRole());
        assertEquals("Ravi Kumar", u.get().getName());
    }

    @Test
    void tamperedRoleIsRejected() {
        String token = SessionToken.create(ravi());
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))));
        String forged = SessionToken.b64(payload.replace("DELIVERY_BOY", "OWNER").getBytes())
                + token.substring(token.indexOf('.'));        // old signature, edited payload
        assertTrue(SessionToken.parse(forged).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = SessionToken.create(ravi(), System.currentTimeMillis() / 1000 - 10);
        assertTrue(SessionToken.parse(expired).isEmpty());
    }

    @Test
    void garbageIsRejectedWithoutException() {
        assertTrue(SessionToken.parse(null).isEmpty());
        assertTrue(SessionToken.parse("").isEmpty());
        assertTrue(SessionToken.parse("abc").isEmpty());
        assertTrue(SessionToken.parse("a.b").isEmpty());
        assertTrue(SessionToken.parse("!!!.???").isEmpty());
    }

    @Test
    void csrfTokenMatchesOnlyItsOwnCookie() {
        String t1 = SessionToken.create(ravi());
        String t2 = SessionToken.create(new SessionUser(8, "Other", Role.CUSTOMER));
        assertTrue(SessionToken.csrfValid(t1, SessionToken.csrfFor(t1)));
        assertFalse(SessionToken.csrfValid(t1, SessionToken.csrfFor(t2)));
        assertFalse(SessionToken.csrfValid(t1, null));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> SessionToken.init("too-short"));
    }
}
