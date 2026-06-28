package com.ticketapp.client.level4;

import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ExternalLevel4Client implements Level4Client {

    private final RestClient restClient;
    private final String qrCodePath;
    private final boolean mockEnabled;

    public ExternalLevel4Client(
            RestClient.Builder builder,
            @Value("${app.level4.base-url:}") String baseUrl,
            @Value("${app.level4.qr-code-path:/qr/generate-dynamic-qr}") String qrCodePath,
            @Value("${app.level4.mock-enabled:true}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.qrCodePath = qrCodePath;
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
