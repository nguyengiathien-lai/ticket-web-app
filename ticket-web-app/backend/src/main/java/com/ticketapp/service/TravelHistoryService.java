package com.ticketapp.service;

import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import com.ticketapp.dto.travel.TravelHistoryResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TravelHistoryService {

    private final Level5Client level5Client;

    public TravelHistoryService(Level5Client level5Client) {
        this.level5Client = level5Client;
    }

    public List<TravelHistoryResponse> getTravelHistoryForPassenger(String passengerAccountId) {
        if (passengerAccountId == null || passengerAccountId.isBlank()) {
            throw new IllegalArgumentException("Passenger account ID is required");
        }

        String normalizedAccountId = passengerAccountId.trim();
        return level5Client.getTravelHistory(normalizedAccountId)
                .stream()
                .map(history -> toTravelHistory(normalizedAccountId, history))
                .sorted(Comparator.comparing(
                        TravelHistoryResponse::getCheckinTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private TravelHistoryResponse toTravelHistory(String passengerAccountId, ExternalTravelHistoryResponse external) {
        return TravelHistoryResponse.builder()
                .externalTripId(external.getExternalTripId())
                .passengerAccountId(firstText(external.getPassengerAccountId(), passengerAccountId))
                .physicalCardExternalId(external.getPhysicalCardExternalId())
                .ticketExternalId(external.getTicketExternalId())
                .checkinStationCode(external.getCheckinStationCode())
                .checkinStationName(external.getCheckinStationName())
                .checkoutStationCode(external.getCheckoutStationCode())
                .checkoutStationName(external.getCheckoutStationName())
                .checkinTime(external.getCheckinTime())
                .checkoutTime(external.getCheckoutTime())
                .transportId(external.getTransportId())
                .transportType(external.getTransportType())
                .routeCode(external.getRouteCode())
                .expiresAt(external.getExpiresAt())
                .build();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
