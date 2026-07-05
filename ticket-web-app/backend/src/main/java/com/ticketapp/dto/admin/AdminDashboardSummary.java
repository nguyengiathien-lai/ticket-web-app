package com.ticketapp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummary {
    private long totalAccounts;
    private long newRegistrationsToday;
    private List<LoginTrafficPoint> loginTrafficToday;
}
