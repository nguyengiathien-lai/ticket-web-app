package com.ticketapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "travel_history")
public class TravelHistory extends BaseEntity {

    @Column(name = "external_trip_id", nullable = false, unique = true, length = 100)
    private String externalTripId;

    @Column(name = "passenger_account_id", nullable = false, length = 36)
    private String passengerAccountId;

    @Column(name = "physical_card_external_id", length = 100)
    private String physicalCardExternalId;

    @Column(name = "ticket_external_id", length = 100)
    private String ticketExternalId;

    @Column(name = "checkin_station_code", nullable = false, length = 50)
    private String checkinStationCode;

    @Column(name = "checkin_station_name", length = 150)
    private String checkinStationName;

    @Column(name = "checkout_station_code", length = 50)
    private String checkoutStationCode;

    @Column(name = "checkout_station_name", length = 150)
    private String checkoutStationName;

    @Column(name = "checkin_time", nullable = false)
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    @Column(name = "transport_id", length = 100)
    private String transportId;

    @Column(name = "transport_type", length = 20)
    private String transportType; // BUS, METRO, TRAIN

    @Column(name = "route_code", length = 50)
    private String routeCode;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
