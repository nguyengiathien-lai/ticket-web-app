package com.ticketapp.dto.fare;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleTripFareQuoteResponse {

    private String mode;
    private String fromStationId;
    private String toStationId;
    private BigDecimal distanceKm;
    private BigDecimal baseFare;
    private BigDecimal ratePerKm;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal price;
    private String currency;
}
