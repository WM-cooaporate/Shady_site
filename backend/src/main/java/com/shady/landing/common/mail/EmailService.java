package com.shady.landing.common.mail;

/** Outgoing email. Implementations: {@link LoggingEmailService} now; Brevo/Resend later. */
public interface EmailService {

    void send(EmailMessage message);
}
