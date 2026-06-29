package com.ticketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.ticket.TicketQrResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketRequestServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private SetOperations<String, String> sets;
    private ObjectMapper mapper;
    private TicketService service;
    private Level5Client level5Client;
    private Level4Client level4Client;

    @BeforeEach
    void setUp() {
        values = mock(ValueOperations.class);
        sets = mock(SetOperations.class);
        mapper = new ObjectMapper().findAndRegisterModules();
        level5Client = mock(Level5Client.class);
        level4Client = mock(Level4Client.class);
        redis = redisTemplate(values, sets);
        service = new TicketService(redis, mapper, level5Client, level4Client);
    }

    @Test
    void cachesAnIdOnlyPurchaseResponseUsingRequestValuesAndDefaults() {
        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId(" ticket-1 ");

        TicketResponse result = service.cacheExternalTicket(request(" account-1 ", " day-pass "), external);

        assertThat(result.getExternalTicketId()).isEqualTo("ticket-1");
        assertThat(result.getPassengerAccountId()).isEqualTo("account-1");
        assertThat(result.getTicketTypeCode()).isEqualTo("day-pass");
        assertThat(result.getTicketCode()).isEqualTo("ticket-1");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCurrency()).isEqualTo("VND");
        assertThat(result.getIssuedAt()).isNotNull();
        assertThat(result.getCachedAt()).isNotNull();
        verify(values).set(eq("cache:tickets:id:ticket-1"), anyString());
    }

    @Test
    void partialRefreshDoesNotEraseExistingRedisData() throws Exception {
        LocalDateTime validUntil = LocalDateTime.now().plusDays(1);
        Ticket cached = new Ticket();
        cached.setExternalTicketId("ticket-1");
        cached.setPassengerAccountId("account-1");
        cached.setTicketTypeCode("day-pass");
        cached.setTicketCode("CODE-1");
        cached.setStatus("ACTIVE");
        cached.setFare(new BigDecimal("50000"));
        cached.setCurrency("VND");
        cached.setRemainingUses(10);
        cached.setValidUntil(validUntil);
        cached.setIssuedAt(LocalDateTime.now().minusHours(1));
        when(values.get("cache:tickets:id:ticket-1")).thenReturn(mapper.writeValueAsString(cached));

        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId("ticket-1");
        TicketResponse result = service.cacheExternalTicket(request("account-1", "day-pass"), external);

        assertThat(result.getTicketCode()).isEqualTo("CODE-1");
        assertThat(result.getFare()).isEqualByComparingTo("50000");
        assertThat(result.getRemainingUses()).isEqualTo(10);
        assertThat(result.getValidUntil()).isEqualTo(validUntil);
        assertThat(result.getExpiresAt()).isEqualTo(validUntil);
    }

    @Test
    void rejectsAnInvalidValidityPeriodBeforeCaching() {
        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId("ticket-1");
        external.setValidFrom(LocalDateTime.now().plusDays(2));
        external.setValidUntil(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.cacheExternalTicket(request("account-1", "day-pass"), external))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("validity period");
        verify(values, never()).set(anyString(), anyString());
    }

    @Test
    void loadsAndCachesTicketsFromLevel5WhenHistoryWasNotLoaded() throws Exception {
        ExternalTicketHistoryResponse external = new ExternalTicketHistoryResponse();
        external.setTicketId("ticket-1");
        external.setType("DAY_PASS");
        external.setStatus("ACTIVE");
        external.setPrice(new BigDecimal("50000"));
        external.setValidFrom(LocalDate.now());
        external.setValidTo(LocalDate.now().plusDays(1));
        external.setPurchasedAt(LocalDateTime.now());
        when(level5Client.getTickets("account-1")).thenReturn(List.of(external));

        Ticket cached = new Ticket();
        cached.setExternalTicketId("ticket-1");
        cached.setPassengerAccountId("account-1");
        cached.setTicketTypeCode("DAY_PASS");
        cached.setTicketCode("ticket-1");
        cached.setStatus("ACTIVE");
        cached.setFare(new BigDecimal("50000"));
        cached.setCurrency("VND");
        cached.setIssuedAt(external.getPurchasedAt());
        when(sets.members("cache:tickets:passenger:account-1")).thenReturn(Set.of("ticket-1"));
        when(values.get("cache:tickets:id:ticket-1")).thenReturn(mapper.writeValueAsString(cached));

        List<TicketResponse> result = service.getTicketsForPassenger("account-1");

        assertThat(result).extracting(TicketResponse::getExternalTicketId).containsExactly("ticket-1");
        verify(level5Client).getTickets("account-1");
    }

    @Test
    void sendsTicketIdToLevel4WhenGeneratingQrCode() {
        QrCodeResponse qrCode = new QrCodeResponse();
        qrCode.setQrCode("AFCQR:v1:ticket-1:exp=9999999999:hmac=test");
        when(level4Client.generateQrCode(org.mockito.ArgumentMatchers.any(QrCodeRequest.class))).thenReturn(qrCode);

        TicketQrResponse result = service.getTicketQrCode("account-1", " ticket-1 ");

        assertThat(result.getTicketId()).isEqualTo("ticket-1");
        assertThat(result.getQrCode()).isEqualTo(qrCode.getQrCode());
        ArgumentCaptor<QrCodeRequest> requestCaptor = ArgumentCaptor.forClass(QrCodeRequest.class);
        verify(level4Client).generateQrCode(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTicketId()).isEqualTo("ticket-1");
        verify(values, never()).get("cache:tickets:id:ticket-1");
    }

    @Test
    void doesNotCheckCachedTicketOwnershipWhenGeneratingQrCode() throws Exception {
        QrCodeResponse qrCode = new QrCodeResponse();
        qrCode.setQrCode("AFCQR:v1:ticket-1:exp=9999999999:hmac=test");
        when(level4Client.generateQrCode(org.mockito.ArgumentMatchers.any(QrCodeRequest.class))).thenReturn(qrCode);

        TicketQrResponse result = service.getTicketQrCode("account-1", "ticket-1");

        assertThat(result.getTicketId()).isEqualTo("ticket-1");
        verify(values, never()).get("cache:tickets:id:ticket-1");
    }

    private TicketRequest request(String passengerAccountId, String ticketTypeCode) {
        TicketRequest request = new TicketRequest();
        request.setPassengerAccountId(passengerAccountId);
        request.setTicketTypeCode(ticketTypeCode);
        return request;
    }

    private StringRedisTemplate redisTemplate(
            ValueOperations<String, String> valueOperations,
            SetOperations<String, String> setOperations) {
        return new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() {
                return valueOperations;
            }

            @Override
            public SetOperations<String, String> opsForSet() {
                return setOperations;
            }
        };
    }
}
