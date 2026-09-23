package com.shady.landing.auth;

import com.shady.landing.common.error.BadRequestException;
import java.nio.charset.StandardCharsets;

/** Admin password rules. BCrypt only uses the first 72 bytes, so longer passwords are rejected. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 64;
    public static final int MAX_BCRYPT_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BadRequestException("Password must be " + MIN_LENGTH + "-" + MAX_LENGTH + " characters");
        }
        if (!fitsBcrypt(password)) {
            throw new BadRequestException("Password is too long");
        }
    }

    public static boolean fitsBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES;
    }
}
