package com.shady.landing.common.error;

/** 401 with a caller-safe message (e.g. the generic login failure message). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
