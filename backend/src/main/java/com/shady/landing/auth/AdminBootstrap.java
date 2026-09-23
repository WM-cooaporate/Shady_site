package com.shady.landing.auth;

import com.shady.landing.common.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the single admin account from ADMIN_EMAIL / ADMIN_INITIAL_PASSWORD on first startup. Once an admin
 * exists these variables are ignored (the password is then changed through the API) and can be removed.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]{1,189}\\.[^@\\s]{2,}$");

    private final AdminUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties.Admin admin;
    private final Clock clock;

    public AdminBootstrap(AdminUserRepository users, PasswordEncoder passwordEncoder, AppProperties properties,
                          Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.admin = properties.admin();
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String email = admin.email() == null ? "" : admin.email().trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalStateException("No admin account exists: set a valid ADMIN_EMAIL and ADMIN_INITIAL_PASSWORD");
        }
        try {
            PasswordPolicy.validate(admin.initialPassword());
        } catch (RuntimeException e) {
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD is invalid: " + e.getMessage());
        }
        users.save(new AdminUser(email, passwordEncoder.encode(admin.initialPassword()), Instant.now(clock)));
        log.info("Created the admin account for {}", email);
    }
}
