package com.ticketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.fare.FarePackageResponse;
import com.ticketapp.entity.FarePackage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketAndCardTypeServiceTest {

    @Test
    void fetchesFarePackagesAgainAfterTheLoadedMarkerExpires() throws Exception {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        Level5Client level5Client = mock(Level5Client.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        StringRedisTemplate redis = redisTemplate(values, sets);

        ExternalFarePriceResponse external = new ExternalFarePriceResponse(
                "METRO",
                new ExternalFarePriceResponse.SingleTripPrice(
                        new BigDecimal("8000"), new BigDecimal("850"), new BigDecimal("8000"), new BigDecimal("30000")),
                List.of(new ExternalFarePriceResponse.PassPriceItem("DAILY", null, null, new BigDecimal("40000"))));
        when(level5Client.getFarePrices()).thenReturn(List.of(external));
        when(values.get("catalog:fare-packages:loaded")).thenReturn(null, "true", null);
        when(sets.members("catalog:fare-packages:codes")).thenReturn(Set.of("METRO_DAILY"));

        FarePackage cached = new FarePackage();
        cached.setCode("METRO_DAILY");
        cached.setName("METRO daily pass");
        cached.setKind("PASS");
        cached.setMode("METRO");
        cached.setDurationType("DAILY");
        cached.setDurationDays(1);
        cached.setPrice(new BigDecimal("40000"));
        cached.setCurrency("VND");
        cached.setActive(true);
        when(values.get("catalog:fare-packages:METRO_DAILY")).thenReturn(mapper.writeValueAsString(cached));

        FarePackageService farePackageService = new FarePackageService(redis, mapper, level5Client, 600);
        TicketService service = new TicketService(redis, mapper, level5Client, null, farePackageService);
        List<FarePackageResponse> first = service.getActiveFarePackages();
        service.getActiveFarePackages();
        service.getActiveFarePackages();

        assertThat(first).extracting(FarePackageResponse::getCode).containsExactly("METRO_DAILY");
        verify(level5Client, times(2)).getFarePrices();
    }

    @Test
    void cardServiceReadsFarePackagesFromSharedCatalog() throws Exception {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        Level5Client level5Client = mock(Level5Client.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        StringRedisTemplate redis = redisTemplate(values, sets);

        ExternalFarePriceResponse external = new ExternalFarePriceResponse(
                "BUS",
                null,
                List.of(new ExternalFarePriceResponse.PassPriceItem(
                        "MONTHLY", 1, "SINGLE_ROUTE", new BigDecimal("140000"))));
        when(level5Client.getFarePrices()).thenReturn(List.of(external));
        when(sets.members("catalog:fare-packages:codes")).thenReturn(Set.of("BUS_MONTHLY_SINGLE_ROUTE"));

        FarePackage cached = new FarePackage();
        cached.setCode("BUS_MONTHLY_SINGLE_ROUTE");
        cached.setName("BUS monthly single_route pass");
        cached.setKind("PASS");
        cached.setMode("BUS");
        cached.setScope("SINGLE_ROUTE");
        cached.setDurationType("MONTHLY");
        cached.setDurationDays(30);
        cached.setDurationMonths(1);
        cached.setPrice(new BigDecimal("140000"));
        cached.setCurrency("VND");
        cached.setActive(true);
        when(values.get("catalog:fare-packages:BUS_MONTHLY_SINGLE_ROUTE"))
                .thenReturn(mapper.writeValueAsString(cached));

        FarePackageService farePackageService = new FarePackageService(redis, mapper, level5Client, 600);
        List<FarePackageResponse> result = new CardService(redis, mapper, level5Client, farePackageService)
                .getActiveFarePackages();

        assertThat(result).extracting(FarePackageResponse::getCode).containsExactly("BUS_MONTHLY_SINGLE_ROUTE");
        verify(level5Client).getFarePrices();
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

            @Override
            public Boolean expire(String key, Duration timeout) {
                return true;
            }
        };
    }
}
