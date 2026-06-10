package com.ticketapp.service;

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
            log.warn("Email sender is not configured. Verification OTP for {} is {}", to, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Verify your Ticket App account");
        message.setText("""
                Hi %s,

                Your Ticket App verification code is: %s

                This code expires in 15 minutes. If you did not create this account, ignore this email.
                """.formatted(fullName, code));

        try {
            mailSender.send(message);
            log.info("Email verification OTP sent to {}", to);
        } catch (MailException exception) {
            log.warn("Email delivery failed for {}. Verification OTP is {}", to, code, exception);
        }
    }
}
