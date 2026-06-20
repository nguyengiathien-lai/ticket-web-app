package com.ticketapp.service;

import com.ticketapp.dto.card.PhysicalCardResponse;
import com.ticketapp.repository.PhysicalCardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PhysicalCardService {

    private final PhysicalCardRepository physicalCardRepository;

    public PhysicalCardService(PhysicalCardRepository physicalCardRepository) {
        this.physicalCardRepository = physicalCardRepository;
    }

    @Transactional(readOnly = true)
    public List<PhysicalCardResponse> getCardsForPassenger(String passengerAccountId) {
        return physicalCardRepository.findByPassengerAccountIdOrderByIssuedAtDesc(passengerAccountId)
                .stream()
                .map(PhysicalCardResponse::from)
                .toList();
    }
}
