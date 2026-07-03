package com.ticketapp.dto.external;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExternalPassengerStationResponse(
        UUID id,
        UUID routeId,
        String code,
        String name,
        BigDecimal kmMarker,
        Integer stationOrder,
        Instant createdAt) {
}
