package com.validationgate.entity;

import com.validationgate.dto.TapEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tap_event")
public class TapEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "ticket_external_id", nullable = false, length = 100)
    private String ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private TapEventType eventType;

    @Column(name = "gate_id", nullable = false, length = 100)
    private String gateId;

    @Column(name = "station_id", nullable = false, length = 100)
    private String stationId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivery_error", length = 500)
    private String deliveryError;
}
