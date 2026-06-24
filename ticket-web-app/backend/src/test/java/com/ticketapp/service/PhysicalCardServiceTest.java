package com.ticketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.card.PhysicalCardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.entity.PhysicalCard;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhysicalCardServiceTest {

    @Test
    void readsPassengerCardsFromRedisWithoutDatabasePersistence() throws Exception {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        Level5Client level5Client = mock(Level5Client.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        StringRedisTemplate redis = redisTemplate(values, sets);

        PhysicalCard card = new PhysicalCard();
        card.setExternalCardId("card-1");
        card.setPassengerAccountId("user-1");
        card.setCardUid("uid-1");
        card.setMaskedCardNumber("**** 1234");
        card.setStatus("ACTIVE");
        card.setIssuedAt(LocalDateTime.of(2026, 6, 20, 12, 0));
        when(sets.members("cache:cards:passenger:user-1")).thenReturn(Set.of("card-1"));
        when(values.get("cache:cards:id:card-1")).thenReturn(mapper.writeValueAsString(card));
        when(values.get("cache:cards:loaded:user-1")).thenReturn("true");

        List<PhysicalCardResponse> result = new PhysicalCardService(redis, mapper, level5Client)
                .getCardsForPassenger("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalCardId()).isEqualTo("card-1");
        assertThat(result.get(0).getMaskedCardNumber()).isEqualTo("**** 1234");
    }

    @Test
    void loadsAndCachesCardsFromLevel5WhenHistoryWasNotLoaded() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        Level5Client level5Client = mock(Level5Client.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        StringRedisTemplate redis = redisTemplate(values, sets);

        ExternalCardHistoryResponse external = new ExternalCardHistoryResponse();
        external.setCardId("card-1");
        external.setCardUid("uid-1");
        external.setStatus("ACTIVE");
        when(level5Client.getCards("user-1")).thenReturn(List.of(external));
        when(sets.members("cache:cards:passenger:user-1")).thenReturn(Set.of("card-1"));
        when(values.get("cache:cards:id:card-1")).thenAnswer(invocation -> {
            PhysicalCard card = new PhysicalCard();
            card.setExternalCardId("card-1");
            card.setPassengerAccountId("user-1");
            card.setCardUid("uid-1");
            card.setStatus("ACTIVE");
            return mapper.writeValueAsString(card);
        });

        List<PhysicalCardResponse> result = new PhysicalCardService(redis, mapper, level5Client)
                .getCardsForPassenger("user-1");

        assertThat(result).extracting(PhysicalCardResponse::getExternalCardId).containsExactly("card-1");
        verify(level5Client).getCards("user-1");
    }

    private StringRedisTemplate redisTemplate(
            ValueOperations<String, String> values,
            SetOperations<String, String> sets) {
        return new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() {
                return values;
            }

            @Override
            public SetOperations<String, String> opsForSet() {
                return sets;
            }
        };
    }
}
