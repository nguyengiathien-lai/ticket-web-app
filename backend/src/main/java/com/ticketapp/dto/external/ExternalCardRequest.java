package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalCardRequest {
    private String passengerAccountId;
    private String cardTypeCode;
    private String orderId;
    private String requestSource;
}
