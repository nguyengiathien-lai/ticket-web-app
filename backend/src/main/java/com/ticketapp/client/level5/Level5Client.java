package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.PurchaseActivityRequest;
import com.ticketapp.dto.external.PurchaseActivityResponse;

public interface Level5Client {
    ExternalTicketResponse requestTicket(ExternalTicketRequest request);

    ExternalCardResponse requestCard(ExternalCardRequest request);

    PurchaseActivityResponse recordPurchase(PurchaseActivityRequest request);
}
