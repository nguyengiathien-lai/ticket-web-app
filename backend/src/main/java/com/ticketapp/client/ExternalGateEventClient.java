package com.ticketapp.client;

import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ExternalGateEventClient implements GateEventClient {

    private final RestClient restClient;
    private final String scanRecordPath;
    private final boolean mockEnabled;

    public ExternalGateEventClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.external-ticket.base-url:}") String baseUrl,
            @Value("${app.external-ticket.scan-record-path:/scan-record}") String scanRecordPath,
            @Value("${app.external-ticket.mock-enabled:true}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank()
                ? restClientBuilder.build()
                : restClientBuilder.baseUrl(baseUrl).build();
        this.scanRecordPath = scanRecordPath;
        this.mockEnabled = mockEnabled;
    }

    @Override
    public ValidationRecordResponse send(ValidationRecordRequest record) {
        if (mockEnabled) {
            return new ValidationRecordResponse("Scan record received successfully");
        }

        ValidationRecordResponse response;
        try {
            response = restClient.post()
                    .uri(scanRecordPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(record)
                    .retrieve()
                    .body(ValidationRecordResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Could not deliver scan record", exception);
        }

        if (response == null || response.getMessage() == null) {
            throw new IllegalStateException("Higher system returned an empty response");
        }
        return response;
    }
}
