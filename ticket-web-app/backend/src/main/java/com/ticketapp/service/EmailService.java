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

        log.info("Sending email verification OTP to {}: {}", to, code);

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
            log.warn("Email delivery failed for {}", validToAddress, exception);
            throw new EmailDeliveryException("Email delivery failed. Check mail configuration and try again", exception);
        }
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
