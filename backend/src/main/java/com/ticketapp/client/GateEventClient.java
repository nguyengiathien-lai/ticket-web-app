package com.ticketapp.client;

import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;

public interface GateEventClient {
    ValidationRecordResponse send(ValidationRecordRequest record);
}
