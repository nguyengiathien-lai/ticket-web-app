package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ExternalPassTicketRequest {

    private String userId;
    private String mode;
    private String scope;
    private String routeId;
    private String cardId;
    private String passengerType;
    private LocalDate validFrom;
    private String durationType;
    private Integer durationMonths;
}
