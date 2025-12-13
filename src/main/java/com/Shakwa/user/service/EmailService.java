package com.Shakwa.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @Value("${mail.enabled:true}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    /**
     * Sends OTP email asynchronously - does not block the caller.
     * Returns a CompletableFuture for optional result handling.
     *
     * @param to      recipient email address
     * @param otpCode the OTP code to send
     * @return CompletableFuture with true if successful, false otherwise
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendOtpEmailAsync(String to, String otpCode) {
        try {
            sendOtpEmailInternal(to, otpCode);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            logger.error("Async email sending failed for: {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Fire-and-forget async email sending - simplest usage.
     * Exceptions are logged but not propagated.
     *
     * @param to      recipient email address
     * @param otpCode the OTP code to send
     */
    @Async("emailTaskExecutor")
    public void sendOtpEmailFireAndForget(String to, String otpCode) {
        try {
            sendOtpEmailInternal(to, otpCode);
        } catch (Exception e) {
            logger.error("Async email sending failed for: {}", to, e);
            // Could add retry logic or dead-letter queue here in the future
        }
    }

    /**
     * Synchronous email sending - blocks until email is sent.
     * Use this when you need immediate feedback on email delivery.
     *
     * @param to      recipient email address
     * @param otpCode the OTP code to send
     */
    public void sendOtpEmail(String to, String otpCode) {
        sendOtpEmailInternal(to, otpCode);
    }

    /**
     * Internal method that performs the actual email sending.
     */
    private void sendOtpEmailInternal(String to, String otpCode) {
        // Development mode: Log OTP instead of sending email
        if (!mailEnabled) {
            logger.warn("========================================");
            logger.warn("MAIL DISABLED - OTP FOR DEVELOPMENT");
            logger.warn("Email: {}", to);
            logger.warn("OTP Code: {}", otpCode);
            logger.warn("========================================");
            return;
        }

        try {
            String htmlContent = buildOtpEmailHtml(otpCode);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setSubject("رمز التحقق من البريد الإلكتروني - Shakwa");
            helper.setText(htmlContent, true); // true => HTML

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", to);

        } catch (MailAuthenticationException e) {
            logger.error("Mail authentication failed. Please check your email credentials.", e);
            throw new RuntimeException("Failed to send OTP email: Mail authentication failed.", e);

        } catch (MailSendException e) {
            logger.error("Failed to send OTP email to: {}. SMTP error.", to, e);
            throw new RuntimeException("Failed to send OTP email: SMTP error.", e);

        } catch (Exception e) {
            logger.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    private String buildOtpEmailHtml(String otpCode) throws IOException, MessagingException {
        ClassPathResource resource = new ClassPathResource("templates/otp-email.html");
        String template = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        return template.replace("{{OTP_CODE}}", otpCode);
    }
}
