package com.shady.landing.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shady.landing.common.mail.EmailMessage;
import com.shady.landing.common.mail.EmailService;
import com.shady.landing.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class LockoutAlertIT extends IntegrationTest {

    @MockitoSpyBean
    EmailService emailService;

    @Test
    void adminIsEmailedWhenTheAccountLocks() throws Exception {
        String ip = uniqueIp();
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").secure(true).header("X-Client-IP", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", ADMIN_EMAIL, "password", "Wrong-Password-" + i))))
                    .andExpect(status().isUnauthorized());
        }

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService, timeout(5_000)).send(sent.capture());
        assertThat(sent.getValue().to()).isEqualTo(ADMIN_EMAIL);
        assertThat(sent.getValue().subject()).contains("locked");
        assertThat(sent.getValue().textBody()).contains(ip);
    }
}
