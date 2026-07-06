package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalPassTicketRequest {

    private String userId;
    private String mode;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String scope;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String routeId;
    private String cardId;
    private String passengerType;
    private LocalDate validFrom;
    private String durationType;
    private Integer durationMonths;
}
