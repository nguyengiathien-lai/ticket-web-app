package com.ticketapp.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketQrResponse {
    private String ticketId;
    private String qrCode;
}
