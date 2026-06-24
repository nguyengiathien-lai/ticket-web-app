package com.ticketapp.client.level4;

import com.ticketapp.dto.external.ExternalGateEventBatchRequest;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.gate.ValidationRecordResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ExternalLevel4Client implements Level4Client {

    private final RestClient restClient;
    private final String qrCodePath;
    private final String scanRecordBatchPath;
    private final boolean mockEnabled;

    public ExternalLevel4Client(
            RestClient.Builder builder,
            @Value("${app.level4.base-url:}") String baseUrl,
            @Value("${app.level4.qr-code-path:/qr-codes}") String qrCodePath,
            @Value("${app.level4.scan-record-batch-path:/scan-record/batch}") String scanRecordBatchPath,
            @Value("${app.level4.mock-enabled:true}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.qrCodePath = qrCodePath;
        this.scanRecordBatchPath = scanRecordBatchPath;
        this.mockEnabled = mockEnabled;
    }

    @Override
    public QrCodeResponse generateQrCode(QrCodeRequest request) {
        if (mockEnabled) {
            QrCodeResponse response = new QrCodeResponse();
            response.setQrCode(request.getTicketId());
            return response;
        }
        return post(qrCodePath, request, QrCodeResponse.class, "QR code generation");
    }

    @Override
    public ValidationRecordResponse sendBatch(ExternalGateEventBatchRequest request) {
        if (mockEnabled) {
            return new ValidationRecordResponse("Batch scan records received successfully");
        }
        return post(scanRecordBatchPath, request, ValidationRecordResponse.class, "scan record batch");
    }

    private <T> T post(String path, Object body, Class<T> responseType, String operation) {
        try {
            T response = restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(responseType);
            if (response == null) {
                throw new IllegalStateException("Level 4 returned an empty response for " + operation);
            }
            return response;
        } catch (RestClientException exception) {
            throw new IllegalStateException("Could not send " + operation + " to Level 4", exception);
        }
    }
}


// package com.ticketapp.client;

// import com.ticketapp.dto.gate.ValidationRecordRequest;
// import com.ticketapp.dto.gate.ValidationRecordResponse;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.MediaType;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestClient;
// import org.springframework.web.client.RestClientException;

// @Component
// public class ExternalGateEventClient implements GateEventClient {

//     private final RestClient restClient;
//     private final String scanRecordPath;
//     private final boolean mockEnabled;

//     public ExternalGateEventClient(
//             RestClient.Builder restClientBuilder,
//             @Value("${app.external-ticket.base-url:}") String baseUrl,
//             @Value("${app.external-ticket.scan-record-path:/scan-record}") String scanRecordPath,
//             @Value("${app.external-ticket.mock-enabled:true}") boolean mockEnabled) {
//         this.restClient = baseUrl.isBlank()
//                 ? restClientBuilder.build()
//                 : restClientBuilder.baseUrl(baseUrl).build();
//         this.scanRecordPath = scanRecordPath;
//         this.mockEnabled = mockEnabled;
//     }

//     @Override
//     public ValidationRecordResponse send(ValidationRecordRequest record) {
//         if (mockEnabled) {
//             return new ValidationRecordResponse("Scan record received successfully");
//         }

//         ValidationRecordResponse response;
//         try {
//             response = restClient.post()
//                     .uri(scanRecordPath)
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .body(record)
//                     .retrieve()
//                     .body(ValidationRecordResponse.class);
//         } catch (RestClientException exception) {
//             throw new IllegalStateException("Could not deliver scan record", exception);
//         }

//         if (response == null || response.getMessage() == null) {
//             throw new IllegalStateException("Higher system returned an empty response");
//         }
//         return response;
//     }
// }
