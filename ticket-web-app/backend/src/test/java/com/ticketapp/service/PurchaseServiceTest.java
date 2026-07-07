package com.ticketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalPassTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.purchase.TicketPurchaseRequest;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Account;
import com.ticketapp.entity.FarePackage;
import com.ticketapp.entity.Order;
import com.ticketapp.entity.Payment;
import com.ticketapp.repository.OrderRepository;
import com.ticketapp.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseServiceTest {

    private AccountService accountService;
    private TicketService ticketService;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private Level5Client level5Client;
    private PurchaseService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        level5Client = mock(Level5Client.class);

        service = new PurchaseService(
                accountService,
                ticketService,
                orderRepository,
                paymentRepository,
                null,
                mock(Level4Client.class),
                level5Client,
                new NoopEmailService());
    }

    @Test
    void monthlyMetroPassSendsNullScopeAndRouteIdToLevel5() throws Exception {
        Account account = new Account();
        account.setId("user-1");
        account.setEmail("user@example.com");
        account.setFullName("Test User");
        account.setIsActive(true);
        account.setIsEmailVerified(true);
        accountService = new StubAccountService(account);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExternalTicketResponse externalTicket = new ExternalTicketResponse();
        externalTicket.setExternalTicketId("ticket-1");
        externalTicket.setPassengerAccountId("user-1");
        externalTicket.setTicketTypeCode("MONTHLY_PASS");
        externalTicket.setMode("METRO");
        externalTicket.setFare(new BigDecimal("200000"));
        externalTicket.setCurrency("VND");
        when(level5Client.purchasePassTicket(any(ExternalPassTicketRequest.class))).thenReturn(externalTicket);
        ticketService = new StubTicketService(TicketResponse.builder()
                .externalTicketId("ticket-1")
                .passengerAccountId("user-1")
                .ticketTypeCode("MONTHLY_PASS")
                .mode("METRO")
                .fare(new BigDecimal("200000"))
                .currency("VND")
                .status("ACTIVE")
                .build());
        service = new PurchaseService(
                accountService,
                ticketService,
                orderRepository,
                paymentRepository,
                null,
                mock(Level4Client.class),
                level5Client,
                new NoopEmailService());

        TicketPurchaseRequest request = new TicketPurchaseRequest();
        request.setUserId("user-1");
        request.setMode("METRO");
        request.setScope("SINGLE_ROUTE");
        request.setRouteId("HN_2A");
        request.setPassengerType("ADULT");
        request.setValidFrom(LocalDate.of(2026, 7, 1));
        request.setDurationType("MONTHLY");
        request.setDurationMonths(1);
        request.setTicketType("PASS");
        service.purchaseTicket(request);

        ArgumentCaptor<ExternalPassTicketRequest> captor =
                ArgumentCaptor.forClass(ExternalPassTicketRequest.class);
        verify(level5Client).purchasePassTicket(captor.capture());
        ExternalPassTicketRequest level5Request = captor.getValue();
        assertThat(level5Request.getMode()).isEqualTo("METRO");
        assertThat(level5Request.getScope()).isNull();
        assertThat(level5Request.getRouteId()).isNull();

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(level5Request);
        assertThat(json).contains("\"scope\":null");
        assertThat(json).contains("\"routeId\":null");
    }

    private static class StubAccountService extends AccountService {

        private final Account account;

        StubAccountService(Account account) {
            this.account = account;
        }

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(account)
                    .filter(value -> value.getId().equals(accountId));
        }
    }

    private static class StubTicketService extends TicketService {

        private final TicketResponse response;

        StubTicketService(TicketResponse response) {
            super(null, null, null, null);
            this.response = response;
        }

        @Override
        public FarePackage requireActiveFarePackage(String code) {
            throw new IllegalArgumentException("Fare package not found");
        }

        @Override
        public TicketResponse cacheExternalTicket(TicketRequest request, ExternalTicketResponse externalTicket) {
            return response;
        }
    }

    private static class NoopEmailService extends EmailService {

        NoopEmailService() {
            super(null, "no-reply@example.com", "Ticket App", "", RestClient.builder());
        }

        @Override
        public void sendTicketPurchaseConfirmed(
                String to,
                String fullName,
                String confirmationNumber,
                String ticketId,
                String packageId,
                BigDecimal totalPrice,
                String currency,
                LocalDateTime purchasedAt) {
        }
    }
}
