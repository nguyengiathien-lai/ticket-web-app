package com.ticketapp.service;

import com.ticketapp.dto.admin.AdminLoginHistoryItem;
import com.ticketapp.entity.Account;
import com.ticketapp.entity.AuditLog;
import com.ticketapp.repository.AccountRepository;
import com.ticketapp.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminLoginHistoryService {

    private static final List<String> LOGIN_ACTIONS = List.of(
            AuditService.LOGIN_SUCCESS,
            AuditService.LOGIN_FAILURE);

    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;

    public AdminLoginHistoryService(
            AuditLogRepository auditLogRepository,
            AccountRepository accountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminLoginHistoryItem> getRecentLoginHistory() {
        List<AuditLog> auditLogs = auditLogRepository.findByActionInOrderByCreatedAtDesc(
                LOGIN_ACTIONS,
                PageRequest.of(0, 100));

        Map<String, Account> accountsById = accountRepository.findAllById(auditLogs.stream()
                        .map(AuditLog::getAccountId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        return auditLogs.stream()
                .map(auditLog -> toHistoryItem(auditLog, Optional.ofNullable(accountsById.get(auditLog.getAccountId()))))
                .toList();
    }

    private AdminLoginHistoryItem toHistoryItem(AuditLog auditLog, Optional<Account> account) {
        String email = account.map(Account::getEmail)
                .orElseGet(() -> auditLog.getAccountId() == null ? auditLog.getResourceId() : null);

        return AdminLoginHistoryItem.builder()
                .id(auditLog.getId())
                .user(account.map(Account::getFullName).orElse(email))
                .email(email)
                .createdAt(auditLog.getCreatedAt())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .result(AuditService.LOGIN_SUCCESS.equals(auditLog.getAction()) ? "SUCCESS" : "FAILURE")
                .build();
    }
}
