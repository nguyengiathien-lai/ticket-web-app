package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
public class ExternalTicketResponse {

    @JsonAlias({"ticketId", "ticketID"})
    private String externalTicketId;
    @JsonAlias("userId")
    private String passengerAccountId;
    @JsonAlias("type")
    private String ticketTypeCode;
    @JsonAlias("cardId")
    private String physicalCardExternalId;
    private String ticketCode;
    private String status;
    private String mode;
    private String scope;
    private String routeId;
    private String fareRuleId;
    private String discountId;
    @JsonAlias("price")
    private BigDecimal fare;
    private String currency;
    private String fromStationCode;
    private String toStationCode;
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime validFrom;
    @JsonAlias("validTo")
    @JsonDeserialize(using = EndOfDayLocalDateTimeDeserializer.class)
    private LocalDateTime validUntil;
    private String qrToken;
    private Boolean expired;
    private Integer remainingUses;
    @JsonAlias("purchasedAt")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime issuedAt;
    @JsonDeserialize(using = EndOfDayLocalDateTimeDeserializer.class)
    private LocalDateTime expiresAt;

    public static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return LocalDateTime.parse(value);
            } catch (RuntimeException ignored) {
                // Try the date-only and offset formats used by the Level 5 ticket API.
            }

            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (RuntimeException ignored) {
                // Fall through to offset parsing.
            }

            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }

    public static class EndOfDayLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return LocalDateTime.parse(value);
            } catch (RuntimeException ignored) {
                // Try the date-only and offset formats used by the Level 5 ticket API.
            }

            try {
                return LocalDate.parse(value).atTime(23, 59, 59);
            } catch (RuntimeException ignored) {
                // Fall through to offset parsing.
            }

            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }
}
