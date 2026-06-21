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
        request.setTicketId(request.getTicketId().trim());
        request.setGateId(request.getGateId().trim());
        request.setStationId(request.getStationId().trim());
        return level4Client.recordScan(request);
    }
}
