package com.ticketapp.service;

import com.ticketapp.entity.TravelHistory;
import com.ticketapp.repository.TravelHistoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelHistoryServiceTest {

    @Test
    void retrievesPassengerJourneysNewestFirst() {
        TravelHistoryRepository repository = mock(TravelHistoryRepository.class);
        TravelHistoryService service = new TravelHistoryService(repository);
        TravelHistory journey = new TravelHistory();
        journey.setExternalTripId("trip-1");
        when(repository.findByPassengerAccountIdOrderByCheckinTimeDesc("user-1"))
                .thenReturn(List.of(journey));

        List<TravelHistory> result = service.getTravelHistoryForPassenger(" user-1 ");

        assertThat(result).containsExactly(journey);
        verify(repository).findByPassengerAccountIdOrderByCheckinTimeDesc("user-1");
    }

    @Test
    void rejectsMissingPassengerId() {
        TravelHistoryService service = new TravelHistoryService(mock(TravelHistoryRepository.class));

        assertThatThrownBy(() -> service.getTravelHistoryForPassenger(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passenger account ID is required");
    }
}
