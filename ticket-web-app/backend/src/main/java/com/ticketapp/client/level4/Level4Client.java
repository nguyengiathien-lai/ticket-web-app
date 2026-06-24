package com.ticketapp.client.level4;

import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;

public interface Level4Client {
    QrCodeResponse generateQrCode(QrCodeRequest request);
}
