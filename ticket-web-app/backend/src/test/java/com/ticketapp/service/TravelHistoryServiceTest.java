package com.ticketapp.service;

import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import com.ticketapp.dto.travel.TravelHistoryResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelHistoryServiceTest {

    @Test
    void retrievesPassengerJourneysNewestFirst() {
        Level5Client level5Client = mock(Level5Client.class);
        TravelHistoryService service = new TravelHistoryService(level5Client);
        ExternalTravelHistoryResponse oldJourney = journey("trip-1", LocalDateTime.parse("2026-01-01T08:00:00"));
        ExternalTravelHistoryResponse newJourney = journey("trip-2", LocalDateTime.parse("2026-01-02T08:00:00"));
        when(level5Client.getTravelHistory("user-1"))
                .thenReturn(List.of(oldJourney, newJourney));

        List<TravelHistoryResponse> result = service.getTravelHistoryForPassenger(" user-1 ");

        assertThat(result).extracting(TravelHistoryResponse::getExternalTripId).containsExactly("trip-2", "trip-1");
        assertThat(result).extracting(TravelHistoryResponse::getPassengerAccountId).containsExactly("user-1", "user-1");
        verify(level5Client).getTravelHistory("user-1");
    }

    @Test
    void rejectsMissingPassengerId() {
        TravelHistoryService service = new TravelHistoryService(mock(Level5Client.class));

        assertThatThrownBy(() -> service.getTravelHistoryForPassenger(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passenger account ID is required");
    }

    private ExternalTravelHistoryResponse journey(String tripId, LocalDateTime checkinTime) {
        ExternalTravelHistoryResponse response = new ExternalTravelHistoryResponse();
        response.setExternalTripId(tripId);
        response.setCheckinTime(checkinTime);
        return response;
    }
}
