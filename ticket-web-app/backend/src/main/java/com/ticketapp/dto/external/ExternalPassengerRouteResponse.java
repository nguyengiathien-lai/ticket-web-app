package com.ticketapp.dto.external;

import java.time.Instant;
import java.util.UUID;

public record ExternalPassengerRouteResponse(
        UUID id,
        UUID operatorId,
        String code,
        String name,
        String type,
        Instant createdAt) {
}
