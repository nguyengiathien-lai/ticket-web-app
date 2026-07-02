package com.ticketapp.dto.external;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExternalDiscountResponse(
        String passengerType,
        String discountType,
        BigDecimal discountValue,
        LocalDate effectiveFrom,
        LocalDate effectiveTo) {
}
