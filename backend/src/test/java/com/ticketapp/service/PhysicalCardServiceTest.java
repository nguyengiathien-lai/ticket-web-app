package com.ticketapp.service;

import com.ticketapp.dto.card.PhysicalCardResponse;
import com.ticketapp.entity.PhysicalCard;
import com.ticketapp.repository.PhysicalCardRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhysicalCardServiceTest {

    @Test
    void mapsPassengerCardsWithoutExposingPersistenceFields() {
        PhysicalCardRepository repository = mock(PhysicalCardRepository.class);
        PhysicalCardService service = new PhysicalCardService(repository);
        PhysicalCard card = new PhysicalCard();
        card.setExternalCardId("card-1");
        card.setPassengerAccountId("user-1");
        card.setCardUid("uid-1");
        card.setMaskedCardNumber("**** 1234");
        card.setStatus("ACTIVE");
        card.setIssuedAt(LocalDateTime.of(2026, 6, 20, 12, 0));
        when(repository.findByPassengerAccountIdOrderByIssuedAtDesc("user-1"))
                .thenReturn(List.of(card));

        List<PhysicalCardResponse> result = service.getCardsForPassenger("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalCardId()).isEqualTo("card-1");
        assertThat(result.get(0).getMaskedCardNumber()).isEqualTo("**** 1234");
    }
}
