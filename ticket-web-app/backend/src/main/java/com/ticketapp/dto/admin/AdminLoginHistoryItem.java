package com.ticketapp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginHistoryItem {
    private Long id;
    private String user;
    private String email;
    private LocalDateTime createdAt;
    private String ipAddress;
    private String userAgent;
    private String result;
}
