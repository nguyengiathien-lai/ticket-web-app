package com.ticketapp.dto.travel;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TravelHistoryResponse {

    private final String externalTripId;
    private final String passengerAccountId;
    private final String physicalCardExternalId;
    private final String ticketExternalId;
    private final String checkinStationCode;
    private final String checkinStationName;
    private final String checkoutStationCode;
    private final String checkoutStationName;
    private final LocalDateTime checkinTime;
    private final LocalDateTime checkoutTime;
    private final String transportId;
    private final String transportType;
    private final String routeCode;
    private final LocalDateTime expiresAt;
}
