package com.uit.petrescueapi.infrastructure.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void formatsExpiry() {
        assertThat(EmailService.formatExpiry(1440)).isEqualTo("24 hours");
        assertThat(EmailService.formatExpiry(60)).isEqualTo("1 hour");
        assertThat(EmailService.formatExpiry(90)).isEqualTo("90 minutes");
        assertThat(EmailService.formatExpiry(5)).isEqualTo("5 minutes");
        assertThat(EmailService.formatExpiry(1)).isEqualTo("1 minute");
    }

    @Test
    void emailsStateTheConfiguredExpiry() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@petrescue.test");
        ReflectionTestUtils.setField(service, "verificationExpiryMinutes", 1440L);
        ReflectionTestUtils.setField(service, "passwordResetExpiryMinutes", 5L);

        service.sendVerificationEmail("a@petrescue.test", "A", "token");
        service.sendPasswordResetEmail("a@petrescue.test", "A", "token");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, org.mockito.Mockito.times(2)).send(sent.capture());
        assertThat(rawBody(sent.getAllValues().get(0))).contains("expires in 24 hours");
        assertThat(rawBody(sent.getAllValues().get(1))).contains("expires in 5 minutes");
    }

    /** Body as sent, with quoted-printable soft line breaks removed. */
    private static String rawBody(MimeMessage message) throws Exception {
        message.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString(StandardCharsets.UTF_8).replace("=\r\n", "");
    }
}
