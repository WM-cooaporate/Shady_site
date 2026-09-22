package com.shady.landing.security;

/**
 * Lets the token validator reject access tokens minted before the admin's credentials last changed
 * (password change) or for an admin that no longer exists. Implemented by the auth module.
 */
public interface CredentialsChangeCheck {

    /** {@code credentialsVersion} is the {@code cv} claim of the token. */
    boolean isTokenStillValid(long adminId, long credentialsVersion);
}
