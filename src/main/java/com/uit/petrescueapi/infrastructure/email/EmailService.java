package com.uit.petrescueapi.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Infrastructure service that sends emails via SMTP (Gmail).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@petrescue.com}")
    private String fromAddress;

    @Value("${app.email.verification-url:http://localhost:8080/api/v1/auth/verify-email}")
    private String verificationBaseUrl;

    @Value("${app.email.password-reset-url:http://localhost:3000/reset-password}")
    private String passwordResetBaseUrl;

    @Value("${app.email.verification-token-expiry-minutes:1440}")
    private long verificationExpiryMinutes;

    @Value("${app.email.password-reset-token-expiry-minutes:60}")
    private long passwordResetExpiryMinutes;

    /**
     * Send verification email asynchronously.
     */
    @Async
    public void sendVerificationEmail(String to, String username, String token) {
        String link = verificationBaseUrl + "?token=" + token;
        String subject = "Verify your Pet Rescue account";
        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                  <h2>Welcome to Pet Rescue, %s!</h2>
                  <p>Thank you for registering. Please verify your email address by clicking the link below:</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 24px;background:#4CAF50;color:#fff;text-decoration:none;border-radius:4px;">Verify Email</a></p>
                  <p>Or copy this link into your browser:</p>
                  <p><code>%s</code></p>
                  <p>This link expires in %s.</p>
                  <br/>
                  <p style="color:#888;">— Pet Rescue Team</p>
                </body>
                </html>
                """.formatted(username, link, link, formatExpiry(verificationExpiryMinutes));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send password reset email asynchronously.
     */
    @Async
    public void  sendPasswordResetEmail(String to, String username, String token) {
        String link = passwordResetBaseUrl + "?token=" + token;
        String subject = "Reset your Pet Rescue password";
        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                  <h2>Password Reset Request</h2>
                  <p>Hello %s,</p>
                  <p>We received a request to reset your password. Click the button below to reset it:</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 24px;background:#FF5722;color:#fff;text-decoration:none;border-radius:4px;">Reset Password</a></p>
                  <p>Or copy this link into your browser:</p>
                  <p><code>%s</code></p>
                  <p><strong>This link expires in %s.</strong></p>
                  <p>If you didn't request this, please ignore this email. Your password will remain unchanged.</p>
                  <br/>
                  <p style="color:#888;">— Pet Rescue Team</p>
                </body>
                </html>
                """.formatted(username, link, link, formatExpiry(passwordResetExpiryMinutes));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    /** Whole hours read as hours, anything else as minutes: 1440 is "24 hours", 90 is "90 minutes". */
    static String formatExpiry(long minutes) {
        if (minutes >= 60 && minutes % 60 == 0) {
            long hours = minutes / 60;
            return hours + (hours == 1 ? " hour" : " hours");
        }
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
