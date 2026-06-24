package com.ticketapp.client.level4;

import com.ticketapp.dto.external.ExternalGateEventBatchRequest;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.gate.ValidationRecordResponse;

public interface Level4Client {
    QrCodeResponse generateQrCode(QrCodeRequest request);

    ValidationRecordResponse sendBatch(ExternalGateEventBatchRequest request);
}
