package com.ticketapp.service;

import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;
import org.springframework.stereotype.Service;

@Service
public class GateValidationService {

    private final Level4Client level4Client;

    public GateValidationService(Level4Client level4Client) {
        this.level4Client = level4Client;
    }

    public ValidationRecordResponse recordValidation(ValidationRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Validation record is required");
        }

        ValidationRecordRequest outboundRecord = new ValidationRecordRequest();
        outboundRecord.setTicketId(requireText(request.getTicketId(), "Ticket ID"));
        outboundRecord.setGateId(requireText(request.getGateId(), "Gate ID"));
        outboundRecord.setStationId(requireText(request.getStationId(), "Station ID"));

        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (request.getRecordedTime() == null) {
            throw new IllegalArgumentException("Recorded time is required");
        }
        outboundRecord.setEventType(request.getEventType());
        outboundRecord.setRecordedTime(request.getRecordedTime());

        ValidationRecordResponse response = level4Client.send(outboundRecord);
        if (response == null || response.getMessage() == null || response.getMessage().isBlank()) {
            throw new IllegalStateException("Level 4 returned an incomplete scan response");
        }
        return response;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
