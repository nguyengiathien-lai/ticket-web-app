package com.ticketapp.service;

import com.ticketapp.exception.EmailDeliveryException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:no-reply@ticketapp.local}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    public void sendEmailVerificationOtp(String to, String fullName, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new EmailDeliveryException("Email sender is not configured");
        }

        String validFromAddress = validateEmailAddress(fromAddress, "Sender email address");
        String validToAddress = validateEmailAddress(to, "Recipient email address");

        log.info("Sending email verification OTP to {}", validToAddress);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(validFromAddress);
            message.setTo(validToAddress);
            message.setSubject("Verify your Ticket App account");
            message.setText("""
                    Hi %s,

                    Your Ticket App verification code is: %s

                    This code expires in 15 minutes. If you did not create this account, ignore this email.
                    """.formatted(fullName, code));

            mailSender.send(message);
            log.info("Email verification OTP sent to {}", validToAddress);
        } catch (MailException exception) {
            String failureReason = failureReason(exception);
            log.warn("Email delivery failed for {}: {}", validToAddress, failureReason, exception);
            throw new EmailDeliveryException("Email delivery failed: " + failureReason, exception);
        }
    }

    public void sendAccountRegistrationConfirmed(String to, String fullName) {
        sendPlainTextEmail(
                to,
                "Your Ticket App account is confirmed",
                """
                        Hi %s,

                        Your Ticket App account registration is complete and your email address has been verified.

                        You can now sign in and start using your account.
                        """.formatted(fullName));
    }

    public void sendTicketPurchaseConfirmed(
            String to,
            String fullName,
            String confirmationNumber,
            String ticketId,
            String packageId,
            BigDecimal totalPrice,
            String currency,
            LocalDateTime purchasedAt) {
        sendPlainTextEmail(
                to,
                "Your ticket purchase is confirmed",
                """
                        Hi %s,

                        Your ticket purchase is confirmed.

                        Confirmation number: %s
                        Ticket ID: %s
                        Package: %s
                        Total: %s %s
                        Purchased at: %s

                        Your QR code is available in your Ticket App account.
                        """.formatted(
                        fullName,
                        confirmationNumber,
                        ticketId,
                        packageId,
                        totalPrice,
                        currency,
                        purchasedAt));
    }

    public void sendCardPurchaseConfirmed(
            String to,
            String fullName,
            String orderId,
            String cardId,
            String packageId,
            String deliveryAddress,
            LocalDate estimatedDelivery,
            BigDecimal totalPrice,
            String currency) {
        sendPlainTextEmail(
                to,
                "Your card purchase is confirmed",
                """
                        Hi %s,

                        Your physical card purchase is confirmed.

                        Order ID: %s
                        Card ID: %s
                        Package: %s
                        Delivery address: %s
                        Estimated delivery: %s
                        Total: %s %s

                        We will keep your card status updated in your Ticket App account.
                        """.formatted(
                        fullName,
                        orderId,
                        cardId,
                        packageId,
                        deliveryAddress,
                        estimatedDelivery,
                        totalPrice,
                        currency));
    }

    private void sendPlainTextEmail(String to, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new EmailDeliveryException("Email sender is not configured");
        }

        String validFromAddress = validateEmailAddress(fromAddress, "Sender email address");
        String validToAddress = validateEmailAddress(to, "Recipient email address");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(validFromAddress);
            message.setTo(validToAddress);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email '{}' sent to {}", subject, validToAddress);
        } catch (MailException exception) {
            String failureReason = failureReason(exception);
            log.warn("Email delivery failed for {}: {}", validToAddress, failureReason, exception);
            throw new EmailDeliveryException("Email delivery failed: " + failureReason, exception);
        }
    }

    private String failureReason(Throwable exception) {
        Throwable current = exception;
        Throwable root = exception;

        while (current != null) {
            root = current;
            current = current.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }

        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }

        return root.getClass().getSimpleName() + ": " + message;
    }

    private String validateEmailAddress(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new EmailDeliveryException(label + " is not configured");
        }

        String trimmedValue = value.trim();
        try {
            InternetAddress internetAddress = new InternetAddress(trimmedValue);
            internetAddress.validate();
            return trimmedValue;
        } catch (AddressException exception) {
            throw new EmailDeliveryException(label + " is invalid: " + trimmedValue, exception);
        }
    }
}
