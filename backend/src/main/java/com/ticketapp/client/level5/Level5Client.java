package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.ExternalTicketTypeResponse;

import java.util.List;

public interface Level5Client {
    ExternalTicketResponse purchaseTicket(ExternalTicketRequest request);

    ExternalCardResponse purchaseCard(ExternalCardRequest request);

    List<ExternalTicketHistoryResponse> getTickets(String accountId);

    List<ExternalCardHistoryResponse> getCards(String accountId);

    List<ExternalTicketTypeResponse> getTicketTypes();

    List<ExternalCardTypeResponse> getCardTypes();
}
