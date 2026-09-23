package com.shady.landing.audit;

public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCOUNT_LOCKED,
    LOGOUT,
    TOKEN_REUSE_DETECTED,
    PASSWORD_CHANGED,
    CREATE,
    UPDATE,
    DELETE
}
