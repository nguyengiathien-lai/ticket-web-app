package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalSingleTripTicketRequest {

    private String userId;
    private String fromStationId;
    private String toStationId;
    private String mode;
}
