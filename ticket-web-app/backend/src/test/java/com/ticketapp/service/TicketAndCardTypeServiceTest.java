package com.ticketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.external.ExternalTicketTypeResponse;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.dto.ticket.TicketTypeResponse;
import com.ticketapp.entity.CardType;
import com.ticketapp.entity.TicketType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketAndCardTypeServiceTest {

    @Test
    void fetchesTicketTypesAgainAfterTheLoadedMarkerExpires() throws Exception {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        Level5Client level5Client = mock(Level5Client.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        StringRedisTemplate redis = redisTemplate(values, sets);

        ExternalTicketTypeResponse external = new ExternalTicketTypeResponse();
        external.setExternalTicketTypeId("type-1");
        external.setCode("DAY_PASS");
        external.setName("Day pass");
        external.setPrice(new BigDecimal("50000"));
        external.setCurrency("VND");
        when(level5Client.getTicketTypes()).thenReturn(List.of(external));
        when(values.get("catalog:ticket-types:loaded")).thenReturn(null, "true", null);
        when(sets.members("catalog:ticket-types:codes")).thenReturn(Set.of("DAY_PASS"));

        TicketType cached = new TicketType();
        cached.setExternalTicketTypeId("type-1");
        cached.setCode("DAY_PASS");
        cached.setName("Day pass");
        cached.setPrice(new BigDecimal("50000"));
        cached.setCurrency("VND");
        cached.setActive(true);
        when(values.get("catalog:ticket-types:DAY_PASS")).thenReturn(mapper.writeValueAsString(cached));

        TicketService service = new TicketService(redis, mapper, level5Client, null, 600);
        List<TicketTypeResponse> first = service.getActiveTicketTypes();
        service.getActiveTicketTypes();
        service.getActiveTicketTypes();

        assertThat(first).extracting(TicketTypeResponse::getCode).containsExactly("DAY_PASS");
        verify(level5Client, times(2)).getTicketTypes();
    }

    @Test
    void fetchesAndCachesCardTypesWhenTheCatalogWasNotLoaded() throws Exception {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        Level5Client level5Client = mock(Level5Client.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        StringRedisTemplate redis = redisTemplate(values, sets);

        ExternalCardTypeResponse external = new ExternalCardTypeResponse();
        external.setExternalCardTypeId("card-type-1");
        external.setCode("STANDARD");
        external.setName("Standard card");
        external.setPrice(new BigDecimal("30000"));
        external.setCurrency("VND");
        when(level5Client.getCardTypes()).thenReturn(List.of(external));
        when(sets.members("catalog:card-types:codes")).thenReturn(Set.of("STANDARD"));

        CardType cached = new CardType();
        cached.setExternalCardTypeId("card-type-1");
        cached.setCode("STANDARD");
        cached.setName("Standard card");
        cached.setPrice(new BigDecimal("30000"));
        cached.setCurrency("VND");
        cached.setActive(true);
        when(values.get("catalog:card-types:STANDARD")).thenReturn(mapper.writeValueAsString(cached));

        List<CardTypeResponse> result = new CardService(redis, mapper, level5Client, 600)
                .getActiveCardTypes();

        assertThat(result).extracting(CardTypeResponse::getCode).containsExactly("STANDARD");
        verify(level5Client).getCardTypes();
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

            @Override
            public Boolean delete(String key) {
                return true;
            }
        };
    }
}
