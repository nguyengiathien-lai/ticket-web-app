package com.ticketapp.client.level4;

import com.ticketapp.dto.external.ExternalGateEventBatchRequest;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;

public interface Level4Client {
    QrCodeResponse generateQrCode(QrCodeRequest request);

    ValidationRecordResponse send(ValidationRecordRequest request);

    ValidationRecordResponse sendBatch(ExternalGateEventBatchRequest request);
}
