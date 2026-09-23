package com.shady.landing.auth;

import java.time.Instant;

public record AdminAccountLockedEvent(long adminId, String email, Instant lockedUntil, String ipAddress) {
}
