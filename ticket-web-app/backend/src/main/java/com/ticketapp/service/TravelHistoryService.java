package com.ticketapp.service;

import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import com.ticketapp.entity.TravelHistory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class TravelHistoryService {

    private final Level5Client level5Client;

    public TravelHistoryService(Level5Client level5Client) {
        this.level5Client = level5Client;
    }

    public List<TravelHistory> getTravelHistoryForPassenger(String passengerAccountId) {
        if (passengerAccountId == null || passengerAccountId.isBlank()) {
            throw new IllegalArgumentException("Passenger account ID is required");
        }

        String normalizedAccountId = passengerAccountId.trim();
        return level5Client.getTravelHistory(normalizedAccountId)
                .stream()
                .map(history -> toTravelHistory(normalizedAccountId, history))
                .sorted(Comparator.comparing(
                        TravelHistory::getCheckinTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private TravelHistory toTravelHistory(String passengerAccountId, ExternalTravelHistoryResponse external) {
        TravelHistory history = new TravelHistory();
        history.setExternalTripId(external.getExternalTripId());
        history.setPassengerAccountId(firstText(external.getPassengerAccountId(), passengerAccountId));
        history.setPhysicalCardExternalId(external.getPhysicalCardExternalId());
        history.setTicketExternalId(external.getTicketExternalId());
        history.setCheckinStationCode(external.getCheckinStationCode());
        history.setCheckinStationName(external.getCheckinStationName());
        history.setCheckoutStationCode(external.getCheckoutStationCode());
        history.setCheckoutStationName(external.getCheckoutStationName());
        history.setCheckinTime(external.getCheckinTime());
        history.setCheckoutTime(external.getCheckoutTime());
        history.setTransportId(external.getTransportId());
        history.setTransportType(external.getTransportType());
        history.setRouteCode(external.getRouteCode());
        history.setCachedAt(LocalDateTime.now());
        history.setExpiresAt(external.getExpiresAt());
        return history;
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
