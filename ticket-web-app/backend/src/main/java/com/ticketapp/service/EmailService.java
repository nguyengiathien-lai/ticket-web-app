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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final String fromName;
    private final String brevoApiKey;
    private final RestClient restClient;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:no-reply@ticketapp.local}") String fromAddress,
            @Value("${app.mail.from-name:Ticket App}") String fromName,
            @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
            RestClient.Builder restClientBuilder) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.brevoApiKey = brevoApiKey;
        this.restClient = restClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .build();
    }

    public void sendEmailVerificationOtp(String to, String fullName, String code) {
        sendPlainTextEmail(
                to,
                "Verify your Ticket App account",
                """
                    Hi %s,

                    Your Ticket App verification code is: %s

                    This code expires in 15 minutes. If you did not create this account, ignore this email.
                    """.formatted(fullName, code));
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
        String validFromAddress = validateEmailAddress(fromAddress, "Sender email address");
        String validToAddress = validateEmailAddress(to, "Recipient email address");

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendWithBrevoApi(validFromAddress, validToAddress, subject, text);
            return;
        }

        sendWithSmtp(validFromAddress, validToAddress, subject, text);
    }

    private void sendWithBrevoApi(String from, String to, String subject, String text) {
        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoApiKey)
                    .body(Map.of(
                            "sender", Map.of(
                                    "name", fromName,
                                    "email", from),
                            "to", List.of(Map.of("email", to)),
                            "subject", subject,
                            "textContent", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email '{}' sent to {} using Brevo API", subject, to);
        } catch (RestClientException exception) {
            String failureReason = failureReason(exception);
            log.warn("Brevo API email delivery failed for {}: {}", to, failureReason, exception);
            throw new EmailDeliveryException("Email delivery failed: " + failureReason, exception);
        }
    }

    private void sendWithSmtp(String from, String to, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new EmailDeliveryException("Email sender is not configured");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email '{}' sent to {} using SMTP", subject, to);
        } catch (MailException exception) {
            String failureReason = failureReason(exception);
            log.warn("SMTP email delivery failed for {}: {}", to, failureReason, exception);
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
