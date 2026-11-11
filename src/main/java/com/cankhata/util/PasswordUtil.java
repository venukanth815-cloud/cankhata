package com.cankhata.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing with BCrypt. BCrypt adds a random salt and is deliberately slow, so stolen hashes are hard to crack.
 * Cost 12 means about 4096 rounds-of-rounds (roughly 250 ms). Lower BCRYPT_COST only for load tests.
 */
public final class PasswordUtil {
    private static final int COST = Math.max(4, Math.min(14, Config.getInt("BCRYPT_COST", 12)));

    /** Checked against when the user does not exist, so a wrong phone number takes as long as a wrong password. */
    public static final String DUMMY_HASH = BCrypt.hashpw("not-a-real-password-1", BCrypt.gensalt(COST));

    private PasswordUtil() {}

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(COST));
    }

    public static boolean verify(String plain, String hash) {
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) {
            return false; // malformed hash
        }
    }
}
