package com.ticketapp.service;

import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Ticket;
import com.ticketapp.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketRequestServiceTest {

    private TicketRepository ticketRepository;
    private TicketRequestService service;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        service = new TicketRequestService(ticketRepository);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cachesAnIdOnlyPurchaseResponseUsingRequestValuesAndDefaults() {
        TicketRequest request = request(" account-1 ", " day-pass ");
        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId(" ticket-1 ");
        when(ticketRepository.findByExternalTicketId(" ticket-1 ")).thenReturn(Optional.empty());

        TicketResponse result = service.cacheExternalTicket(request, external);

        assertThat(result.getExternalTicketId()).isEqualTo("ticket-1");
        assertThat(result.getPassengerAccountId()).isEqualTo("account-1");
        assertThat(result.getTicketTypeCode()).isEqualTo("day-pass");
        assertThat(result.getTicketCode()).isEqualTo("ticket-1");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCurrency()).isEqualTo("VND");
        assertThat(result.getIssuedAt()).isNotNull();
        assertThat(result.getCachedAt()).isNotNull();
    }

    @Test
    void partialRefreshDoesNotEraseExistingTicketData() {
        LocalDateTime validUntil = LocalDateTime.now().plusDays(1);
        Ticket cached = new Ticket();
        cached.setExternalTicketId("ticket-1");
        cached.setPassengerAccountId("account-1");
        cached.setTicketTypeCode("day-pass");
        cached.setTicketCode("CODE-1");
        cached.setStatus("ACTIVE");
        cached.setFare(new BigDecimal("50000"));
        cached.setCurrency("VND");
        // cached.setRemainingUses(10);
        cached.setValidUntil(validUntil);
        cached.setIssuedAt(LocalDateTime.now().minusHours(1));

        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId("ticket-1");
        when(ticketRepository.findByExternalTicketId("ticket-1")).thenReturn(Optional.of(cached));

        TicketResponse result = service.cacheExternalTicket(request("account-1", "day-pass"), external);

        assertThat(result.getTicketCode()).isEqualTo("CODE-1");
        assertThat(result.getFare()).isEqualByComparingTo("50000");
        assertThat(result.getRemainingUses()).isEqualTo(10);
        assertThat(result.getValidUntil()).isEqualTo(validUntil);
        assertThat(result.getExpiresAt()).isEqualTo(validUntil);
    }

    @Test
    void rejectsAnInvalidValidityPeriodBeforeSaving() {
        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId("ticket-1");
        external.setValidFrom(LocalDateTime.now().plusDays(2));
        external.setValidUntil(LocalDateTime.now().plusDays(1));
        when(ticketRepository.findByExternalTicketId("ticket-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cacheExternalTicket(request("account-1", "day-pass"), external))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("validity period");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    private TicketRequest request(String passengerAccountId, String ticketTypeCode) {
        TicketRequest request = new TicketRequest();
        request.setPassengerAccountId(passengerAccountId);
        request.setTicketTypeCode(ticketTypeCode);
        return request;
    }
}
