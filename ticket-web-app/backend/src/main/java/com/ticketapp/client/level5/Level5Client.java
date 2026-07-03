package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalDiscountResponse;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.external.ExternalPassengerRouteResponse;
import com.ticketapp.dto.external.ExternalPassengerStationResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;

import java.util.List;

public interface Level5Client {
    ExternalTicketResponse purchaseTicket(ExternalTicketRequest request);

    ExternalCardResponse purchaseCard(ExternalCardRequest request);

    List<ExternalCardHistoryResponse> getCards(String userId);

    List<ExternalTicketHistoryResponse> getTickets(String userId);

    List<ExternalTravelHistoryResponse> getTravelHistory(String userId);

    List<ExternalPassengerStationResponse> getStations();

    List<ExternalPassengerRouteResponse> getRoutes();

    List<ExternalFarePriceResponse> getFarePrices();

    List<ExternalDiscountResponse> getFareDiscounts();
}
