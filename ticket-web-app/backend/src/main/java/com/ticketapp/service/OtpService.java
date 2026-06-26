package com.ticketapp.service;

import com.ticketapp.entity.Account;
import com.ticketapp.entity.OtpCode;
import com.ticketapp.repository.OtpCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class OtpService {

    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_BOUND = 1_000_000;

    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;

    public OtpService(OtpCodeRepository otpCodeRepository, EmailService emailService) {
        this.otpCodeRepository = otpCodeRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendEmailVerificationOtp(Account account) {
        invalidateUnusedCodes(account, EMAIL_VERIFICATION);

        String code = String.format("%06d", SECURE_RANDOM.nextInt(OTP_BOUND));

        OtpCode otpCode = new OtpCode();
        otpCode.setId(UUID.randomUUID().toString());
        otpCode.setAccount(account);
        otpCode.setCode(code);
        otpCode.setType(EMAIL_VERIFICATION);
        otpCode.setAttemptCount(0);
        otpCode.setMaxAttempts(3);
        otpCode.setIsUsed(false);
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(2));

        otpCodeRepository.save(otpCode);
        emailService.sendEmailVerificationOtp(account.getEmail(), account.getFullName(), code);
    }

    @Transactional
    public boolean verifyEmailOtp(Account account, String code) {
        OtpCode otpCode = otpCodeRepository
                .findFirstByAccountAndTypeAndIsUsedFalseOrderByCreatedAtDesc(account, EMAIL_VERIFICATION)
                .orElseThrow(() -> new IllegalArgumentException("Verification code not found or already used"));

        if (otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpCode.setIsUsed(true);
            otpCode.setUsedAt(LocalDateTime.now());
            otpCodeRepository.save(otpCode);
            throw new IllegalArgumentException("Verification code has expired");
        }

        if (otpCode.getAttemptCount() >= otpCode.getMaxAttempts()) {
            otpCode.setIsUsed(true);
            otpCode.setUsedAt(LocalDateTime.now());
            otpCodeRepository.save(otpCode);
            throw new IllegalArgumentException("Verification code attempt limit exceeded");
        }

        if (!otpCode.getCode().equals(code)) {
            otpCode.setAttemptCount(otpCode.getAttemptCount() + 1);
            otpCodeRepository.save(otpCode);
            throw new IllegalArgumentException("Verification code is incorrect");
        }

        otpCode.setIsUsed(true);
        otpCode.setUsedAt(LocalDateTime.now());
        otpCodeRepository.save(otpCode);
        return true;
    }

    private void invalidateUnusedCodes(Account account, String type) {
        LocalDateTime now = LocalDateTime.now();
        otpCodeRepository.findByAccountAndTypeAndIsUsedFalse(account, type)
                .forEach(otpCode -> {
                    otpCode.setIsUsed(true);
                    otpCode.setUsedAt(now);
                    otpCodeRepository.save(otpCode);
                });
    }
}
