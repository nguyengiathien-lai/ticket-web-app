package com.ticketapp.dto.external;

import java.math.BigDecimal;
import java.util.List;

public record ExternalFarePriceResponse(
        String mode,
        SingleTripPrice singleTrip,
        List<PassPriceItem> passPrices) {

    public record SingleTripPrice(
            BigDecimal baseFare,
            BigDecimal ratePerKm,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
    }

    public record PassPriceItem(
            String durationType,
            Integer durationMonths,
            String scope,
            BigDecimal price) {
    }
}
