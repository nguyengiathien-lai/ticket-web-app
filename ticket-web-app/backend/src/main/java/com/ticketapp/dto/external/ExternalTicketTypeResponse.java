package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalTicketTypeResponse {

    @JsonAlias({"id", "ticketTypeId"})
    private String externalTicketTypeId;
    private String code;
    private String name;
    private Integer durationDays;
    private BigDecimal price;
    private String currency;
    private String description;
    private Boolean active;
    private LocalDateTime expiresAt;
}
