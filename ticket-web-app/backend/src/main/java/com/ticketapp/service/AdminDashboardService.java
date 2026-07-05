package com.ticketapp.service;

import com.ticketapp.dto.admin.AdminDashboardSummary;
import com.ticketapp.dto.admin.LoginTrafficPoint;
import com.ticketapp.entity.AuditLog;
import com.ticketapp.repository.AccountRepository;
import com.ticketapp.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardService {

    private static final DateTimeFormatter HOUR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardService(
            AccountRepository accountRepository,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummary getTodaySummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        return AdminDashboardSummary.builder()
                .totalAccounts(accountRepository.count())
                .newRegistrationsToday(accountRepository.countByCreatedAtBetween(startOfDay, startOfTomorrow))
                .loginTrafficToday(buildHourlyLoginTraffic(startOfDay, startOfTomorrow))
                .build();
    }

    private List<LoginTrafficPoint> buildHourlyLoginTraffic(LocalDateTime start, LocalDateTime end) {
        long[] loginCountsByHour = new long[24];
        List<AuditLog> loginEvents = auditLogRepository.findByActionAndCreatedAtBetweenOrderByCreatedAtAsc(
                AuditService.LOGIN_SUCCESS,
                start,
                end);

        for (AuditLog event : loginEvents) {
            loginCountsByHour[event.getCreatedAt().getHour()]++;
        }

        List<LoginTrafficPoint> traffic = new ArrayList<>();
        for (int hour = 0; hour < loginCountsByHour.length; hour++) {
            traffic.add(LoginTrafficPoint.builder()
                    .time(start.plusHours(hour).format(HOUR_LABEL_FORMATTER))
                    .logins(loginCountsByHour[hour])
                    .build());
        }

        return traffic;
    }
}
