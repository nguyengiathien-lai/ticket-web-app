package com.ticketapp.dto.ticket;

import com.ticketapp.entity.TicketType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketTypeResponse {

    private String externalTicketTypeId;
    private String code;
    private String name;
    private Integer durationDays;
    private BigDecimal price;
    private String currency;
    private String description;
    private Boolean active;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public static TicketTypeResponse from(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .externalTicketTypeId(ticketType.getExternalTicketTypeId())
                .code(ticketType.getCode())
                .name(ticketType.getName())
                .durationDays(ticketType.getDurationDays())
                .price(ticketType.getPrice())
                .currency(ticketType.getCurrency())
                .description(ticketType.getDescription())
                .active(ticketType.getActive())
                .cachedAt(ticketType.getCachedAt())
                .expiresAt(ticketType.getExpiresAt())
                .build();
    }
}
