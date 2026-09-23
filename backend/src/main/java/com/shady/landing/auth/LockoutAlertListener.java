package com.shady.landing.auth;

import com.shady.landing.common.mail.EmailMessage;
import com.shady.landing.common.mail.EmailService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Emails the admin when the account gets locked, after the lock has been committed. */
@Component
public class LockoutAlertListener {

    private static final Logger log = LoggerFactory.getLogger(LockoutAlertListener.class);
    private static final DateTimeFormatter CAIRO_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.of("Africa/Cairo"));

    private final EmailService emailService;

    public LockoutAlertListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @TransactionalEventListener
    public void onLocked(AdminAccountLockedEvent event) {
        String body = """
                Your Shady admin account was locked after too many failed sign-in attempts.

                Locked until: %s
                Last attempt from IP: %s

                If this was not you, change your password as soon as the lock expires.
                """.formatted(CAIRO_TIME.format(event.lockedUntil()), event.ipAddress() == null ? "unknown" : event.ipAddress());
        try {
            emailService.send(new EmailMessage(event.email(), "Shady admin: account locked", body));
        } catch (RuntimeException e) {
            log.error("Could not send lockout alert for admin {}", event.adminId(), e);
        }
    }
}
