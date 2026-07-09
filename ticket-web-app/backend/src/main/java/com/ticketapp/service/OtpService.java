package com.ticketapp.service;

import com.ticketapp.entity.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class OtpService {

    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_BOUND = 1_000_000;
    private static final int MAX_ATTEMPTS = 3;
    private static final String KEY_PREFIX = "ticketapp:otp:email-verification:";

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final Duration emailVerificationTtl;

    public OtpService(
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            @Value("${app.otp.email-verification-ttl:2m}") Duration emailVerificationTtl) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.emailVerificationTtl = emailVerificationTtl;
    }

    public void sendEmailVerificationOtp(Account account) {
        String code = String.format("%06d", SECURE_RANDOM.nextInt(OTP_BOUND));
        String key = key(account.getId());

        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(key, Map.of(
                "code", code,
                "attemptCount", "0",
                "maxAttempts", String.valueOf(MAX_ATTEMPTS)));
        redisTemplate.expire(key, emailVerificationTtl);
        log.info("Saved email verification OTP in Redis for {}", account.getEmail());

        log.info("Sending email verification OTP to {}", account.getEmail());
        emailService.sendEmailVerificationOtp(account.getEmail(), account.getFullName(), code);
    }

    public boolean verifyEmailOtp(Account account, String code) {
        String key = key(account.getId());
        Map<Object, Object> otp = redisTemplate.opsForHash().entries(key);
        if (otp.isEmpty()) {
            throw new IllegalArgumentException("Verification code not found, expired, or already used");
        }

        int attemptCount = integer(otp.get("attemptCount"));
        int maxAttempts = integer(otp.get("maxAttempts"));
        if (attemptCount >= maxAttempts) {
            redisTemplate.delete(key);
            throw new IllegalArgumentException("Verification code attempt limit exceeded");
        }

        if (!String.valueOf(otp.get("code")).equals(code)) {
            redisTemplate.opsForHash().put(key, "attemptCount", String.valueOf(attemptCount + 1));
            throw new IllegalArgumentException("Verification code is incorrect");
        }

        redisTemplate.delete(key);
        return true;
    }

    public void deleteEmailVerificationOtp(String accountId) {
        redisTemplate.delete(key(accountId));
    }

    private String key(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID is required for OTP");
        }
        return KEY_PREFIX + accountId;
    }

    private int integer(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid OTP data in Redis", exception);
        }
    }
}
