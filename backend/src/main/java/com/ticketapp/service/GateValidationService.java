package com.ticketapp.service;

import com.ticketapp.client.GateEventClient;
import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;
import org.springframework.stereotype.Service;

@Service
public class GateValidationService {

    private final GateEventClient gateEventClient;

    public GateValidationService(GateEventClient gateEventClient) {
        this.gateEventClient = gateEventClient;
    }

    public ValidationRecordResponse recordValidation(ValidationRecordRequest request) {
        request.setTicketId(request.getTicketId().trim());
        request.setGateId(request.getGateId().trim());
        request.setStationId(request.getStationId().trim());
        return gateEventClient.send(request);
    }
}
