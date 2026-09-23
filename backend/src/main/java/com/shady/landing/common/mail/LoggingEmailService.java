package com.shady.landing.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default provider ({@code MAIL_PROVIDER=log}): logs the email instead of sending it. A Brevo/Resend
 * implementation will be selected with a different {@code app.mail.provider} value.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void send(EmailMessage message) {
        log.info("[email not sent: logging provider] to={} subject=\"{}\"\n{}",
                message.to(), message.subject(), message.textBody());
    }
}
