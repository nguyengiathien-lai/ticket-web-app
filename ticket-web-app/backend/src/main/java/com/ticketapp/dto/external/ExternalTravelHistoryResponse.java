package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalTravelHistoryResponse {

    @JsonAlias({"travelId", "tripId", "externalTripId"})
    private String externalTripId;

    private String passengerAccountId;

    // @JsonAlias({"cardId", "externalCardId", "physicalCardExternalId"})
    // private String physicalCardExternalId;

    private String mode;

    @JsonAlias({"ticketId", "externalTicketId", "ticketExternalId"})
    private String ticketExternalId;

    @JsonAlias({"tapInStationCode", "checkInStationCode", "checkinStationCode"})
    private String checkinStationCode;

    // @JsonAlias({"originStationName", "checkInStationName", "checkinStationName"})
    // private String checkinStationName;

    @JsonAlias({"tapOutStationCode", "checkOutStationCode", "checkoutStationCode"})
    private String checkoutStationCode;

    // @JsonAlias({"destinationStationName", "checkOutStationName", "checkoutStationName"})
    // private String checkoutStationName;

    @JsonAlias({"checkInTime", "tapInTime", "tapIndAt", "checkinTime"})
    private LocalDateTime checkinTime;

    @JsonAlias({"checkOutTime", "tapOutTime", "tapOutAt", "checkoutTime"})
    private LocalDateTime checkoutTime;

    // private String transportId;
    // private String transportType;
    // private String routeCode;
    // private LocalDateTime expiresAt;
    private BigDecimal distanceKm;
    private BigDecimal fareAmount;
}
