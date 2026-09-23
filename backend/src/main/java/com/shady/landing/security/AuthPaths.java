package com.shady.landing.security;

public final class AuthPaths {

    public static final String BASE = "/api/v1/auth";
    public static final String LOGIN = BASE + "/login";
    public static final String REFRESH = BASE + "/refresh";
    public static final String LOGOUT = BASE + "/logout";
    public static final String PUBLIC = "/api/v1/public/**";
    public static final String PUBLIC_CLICKS = "/api/v1/public/clicks";
    public static final String ADMIN = "/api/v1/admin/**";

    private AuthPaths() {
    }
}
