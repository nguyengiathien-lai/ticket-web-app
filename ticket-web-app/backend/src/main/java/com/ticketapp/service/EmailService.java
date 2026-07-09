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
                "Xác nhận địa chỉ email của bạn",
                """
                    Xin chào %s,

                    Mã xác nhận Ticket App của bạn là: %s

                    Mã này sẽ hết hạn sau 2 phút. Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.
                    """.formatted(fullName, code));
    }

    public void sendAccountRegistrationConfirmed(String to, String fullName) {
        sendPlainTextEmail(
                to,
                "XÁC NHẬN ĐĂNG KÝ TÀI KHOẢN",
                """
                        Xin chào %s,

                        Đăng ký tài khoản Ticket App của bạn đã hoàn tất và địa chỉ email của bạn đã được xác minh.

                        Bạn hiện có thể đăng nhập vào ứng dụng Ticket App để bắt đầu khám phá các chuyến đi và trải nghiệm tuyệt vời!
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
                "XÁC NHẬN MUA VÉ",
                """
                        Xin chào %s,

                        Đăng ký mua vé của bạn đã được xác nhận.

                        Confirmation number: %s
                        Mã vé: %s
                        Gói vé: %s
                        Tổng tiền: %s %s
                        Ngày mua: %s

                        Mã QR của vé có sẵn trong tài khoản Ticket App của bạn. 
                        Lưu ý: Mã QR chỉ có hiệu lực 30s. Nếu mã hết hạn, vui lòng yêu cầu mã QR mới trong ứng dụng Ticket App.

                        Chúc bạn có những chuyến đi vui vẻ và trải nghiệm tuyệt vời với Ticket App!
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
                "XÁC NHẬN MUA THẺ VẬT LÝ",
                """
                        Xin chào %s,

                        Đăng ký mua thẻ vật lý của bạn đã được xác nhận. 

                        Mã đơn hàng: %s
                        Mã thẻ: %s
                        Gói thẻ: %s
                        Địa chỉ giao hàng: %s
                        Ngày giao dự kiến: %s
                        Tổng tiền thanh toán: %s %s

                        Hiện bạn đã có thể sử dụng gói vé trong thẻ thông qua ứng dụng Ticket App. Thẻ vật lý sẽ được giao đến địa chỉ của bạn trong thời gian sớm nhất.

                        Chúc bạn có những chuyến đi vui vẻ và trải nghiệm tuyệt vời với Ticket App!
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
