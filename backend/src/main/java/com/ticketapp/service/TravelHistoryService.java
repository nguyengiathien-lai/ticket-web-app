package com.ticketapp.service;

import com.ticketapp.entity.TravelHistory;
import com.ticketapp.repository.TravelHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelHistoryService {

    private final TravelHistoryRepository travelHistoryRepository;

    public TravelHistoryService(TravelHistoryRepository travelHistoryRepository) {
        this.travelHistoryRepository = travelHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelHistory> getTravelHistoryForPassenger(String passengerAccountId) {
        if (passengerAccountId == null || passengerAccountId.isBlank()) {
            throw new IllegalArgumentException("Passenger account ID is required");
        }

        return travelHistoryRepository
                .findByPassengerAccountIdOrderByCheckinTimeDesc(passengerAccountId.trim());
    }
}
