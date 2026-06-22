package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;

public interface Level5Client {
    ExternalTicketResponse purchaseTicket(ExternalTicketRequest request);

    ExternalCardResponse purchaseCard(ExternalCardRequest request);
}
