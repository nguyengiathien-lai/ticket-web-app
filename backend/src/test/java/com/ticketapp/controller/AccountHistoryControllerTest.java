package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.card.PhysicalCardResponse;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Account;
import com.ticketapp.entity.TravelHistory;
import com.ticketapp.service.AccountService;
import com.ticketapp.service.AuthenticationService;
import com.ticketapp.service.AuthorizationService;
import com.ticketapp.service.PhysicalCardService;
import com.ticketapp.service.TicketRequestService;
import com.ticketapp.service.TravelHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountHistoryControllerTest {

    private final FakeAccountService accountService = new FakeAccountService();
    private final FakeAuthenticationService authenticationService = new FakeAuthenticationService();
    private final FakeAuthorizationService authorizationService = new FakeAuthorizationService();
    private final FakeTicketRequestService ticketRequestService = new FakeTicketRequestService();
    private final FakePhysicalCardService physicalCardService = new FakePhysicalCardService();
    private final FakeTravelHistoryService travelHistoryService = new FakeTravelHistoryService();
    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(
                accountService,
                authenticationService,
                authorizationService,
                ticketRequestService,
                physicalCardService,
                travelHistoryService);
    }

    @Test
    void passengerCanRetrieveOwnPastTickets() {
        authenticationService.account = Optional.of(account("user-1"));
        TicketResponse ticket = TicketResponse.builder().externalTicketId("ticket-1").build();
        ticketRequestService.tickets = List.of(ticket);

        ResponseEntity<ApiResponse<List<TicketResponse>>> response =
                controller.getPastTickets("Bearer token", "user-1");

        assertThat(authenticationService.receivedToken).isEqualTo("token");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(ticket);
        assertThat(authorizationService.adminCheckCalled).isFalse();
    }

    @Test
    void passengerCanRetrieveOwnPastCards() {
        authenticationService.account = Optional.of(account("user-1"));
        PhysicalCardResponse card = PhysicalCardResponse.builder().externalCardId("card-1").build();
        physicalCardService.cards = List.of(card);

        ResponseEntity<ApiResponse<List<PhysicalCardResponse>>> response =
                controller.getPastCards("token", "user-1");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(card);
    }

    @Test
    void passengerCannotRetrieveAnotherAccountsHistory() {
        authenticationService.account = Optional.of(account("user-1"));
        authorizationService.allowAdmin = false;

        assertThatThrownBy(() -> controller.getPastTickets("Bearer token", "user-2"))
                .isInstanceOf(SecurityException.class);

        assertThat(ticketRequestService.requestedAccountId).isNull();
    }

    @Test
    void passengerCanRetrieveOwnTravelHistory() {
        authenticationService.account = Optional.of(account("user-1"));
        TravelHistory journey = new TravelHistory();
        journey.setExternalTripId("trip-1");
        travelHistoryService.history = List.of(journey);

        ResponseEntity<ApiResponse<List<TravelHistory>>> response =
                controller.getTravelHistory("Bearer token", "user-1");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsExactly(journey);
        assertThat(travelHistoryService.requestedAccountId).isEqualTo("user-1");
    }

    private Account account(String id) {
        Account account = new Account();
        account.setId(id);
        return account;
    }

    private static class FakeAccountService extends AccountService {
        @Override
        public Optional<Account> findById(String accountId) {
            Account account = new Account();
            account.setId(accountId);
            return Optional.of(account);
        }
    }

    private static class FakeAuthenticationService extends AuthenticationService {
        private Optional<Account> account = Optional.empty();
        private String receivedToken;

        @Override
        public Optional<Account> getAuthenticatedAccountByToken(String token) {
            receivedToken = token;
            return account;
        }
    }

    private static class FakeAuthorizationService extends AuthorizationService {
        private boolean allowAdmin = true;
        private boolean adminCheckCalled;

        @Override
        public void requireAdmin(String accountId) {
            adminCheckCalled = true;
            if (!allowAdmin) {
                throw new SecurityException("Role required: APP_ADMIN");
            }
        }
    }

    private static class FakeTicketRequestService extends TicketRequestService {
        private List<TicketResponse> tickets = List.of();
        private String requestedAccountId;

        private FakeTicketRequestService() {
            super(null, null, null);
        }

        @Override
        public List<TicketResponse> getCachedTicketsForPassenger(String passengerAccountId) {
            requestedAccountId = passengerAccountId;
            return tickets;
        }
    }

    private static class FakePhysicalCardService extends PhysicalCardService {
        private List<PhysicalCardResponse> cards = List.of();

        private FakePhysicalCardService() {
            super(null);
        }

        @Override
        public List<PhysicalCardResponse> getCardsForPassenger(String passengerAccountId) {
            return cards;
        }
    }

    private static class FakeTravelHistoryService extends TravelHistoryService {
        private List<TravelHistory> history = List.of();
        private String requestedAccountId;

        private FakeTravelHistoryService() {
            super(null);
        }

        @Override
        public List<TravelHistory> getTravelHistoryForPassenger(String passengerAccountId) {
            requestedAccountId = passengerAccountId;
            return history;
        }
    }
}
