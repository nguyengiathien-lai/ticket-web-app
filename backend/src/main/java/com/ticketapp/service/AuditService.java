package com.ticketapp.service;

import com.ticketapp.entity.AuditLog;
import com.ticketapp.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String LOGOUT_SUCCESS = "LOGOUT_SUCCESS";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordAuthenticationActivity(
            String accountId,
            String attemptedEmail,
            String action,
            String details) {
        HttpServletRequest request = currentRequest();

        AuditLog auditLog = new AuditLog();
        auditLog.setAccountId(accountId);
        auditLog.setAction(action);
        auditLog.setResourceType("ACCOUNT");
        auditLog.setResourceId(accountId != null ? accountId : normalizeEmail(attemptedEmail));
        auditLog.setDetails(details);
        auditLog.setIpAddress(request == null ? null : truncate(request.getRemoteAddr(), 45));
        auditLog.setUserAgent(request == null ? null : truncate(request.getHeader("User-Agent"), 500));
        return auditLogRepository.save(auditLog);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return truncate(email.trim().toLowerCase(), 100);
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }
}
