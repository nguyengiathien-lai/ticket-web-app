package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalTravelHistoryResponse {

    @JsonAlias({"travelId", "tripId", "externalTripId"})
    private String externalTripId;

    private String passengerAccountId;

    @JsonAlias({"cardId", "externalCardId", "physicalCardExternalId"})
    private String physicalCardExternalId;

    @JsonAlias({"ticketId", "externalTicketId", "ticketExternalId"})
    private String ticketExternalId;

    @JsonAlias({"originStationCode", "checkInStationCode", "checkinStationCode"})
    private String checkinStationCode;

    @JsonAlias({"originStationName", "checkInStationName", "checkinStationName"})
    private String checkinStationName;

    @JsonAlias({"destinationStationCode", "checkOutStationCode", "checkoutStationCode"})
    private String checkoutStationCode;

    @JsonAlias({"destinationStationName", "checkOutStationName", "checkoutStationName"})
    private String checkoutStationName;

    @JsonAlias({"checkInTime", "tapInTime", "startedAt", "checkinTime"})
    private LocalDateTime checkinTime;

    @JsonAlias({"checkOutTime", "tapOutTime", "completedAt", "checkoutTime"})
    private LocalDateTime checkoutTime;

    private String transportId;
    private String transportType;
    private String routeCode;
    private LocalDateTime expiresAt;
}
