package com.ticketapp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TravelHistory extends BaseEntity {

    private String externalTripId;

    private String passengerAccountId;

    private String physicalCardExternalId;

    private String ticketExternalId;

    private String checkinStationCode;

    private String checkinStationName;

    private String checkoutStationCode;

    private String checkoutStationName;

    private LocalDateTime checkinTime;

    private LocalDateTime checkoutTime;

    private String transportId;

    private String transportType; // BUS, METRO, TRAIN

    private String routeCode;

    private LocalDateTime cachedAt;

    private LocalDateTime expiresAt;
}
