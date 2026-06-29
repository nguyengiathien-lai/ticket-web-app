package com.ticketapp.client.level4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class ExternalLevel4Client implements Level4Client {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String qrCodePath;
    private final boolean mockEnabled;

    public ExternalLevel4Client(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.level4.base-url:}") String baseUrl,
            @Value("${app.level4.qr-code-path:/vdt/generate-dynamic-qr}") String qrCodePath,
            @Value("${app.level4.mock-enabled:true}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.qrCodePath = qrCodePath;
        this.mockEnabled = mockEnabled;
    }

    @Override
    public QrCodeResponse generateQrCode(QrCodeRequest request) {
        log.info("Sending QR code generation request to Level 4; mockEnabled={}, baseUrl={}, path={}, ticketId={}",
                mockEnabled, baseUrl, qrCodePath, request == null ? null : request.getTicketId());
        if (mockEnabled) {
            QrCodeResponse response = new QrCodeResponse();
            response.setQrCode(request.getTicketId());
            return response;
        }
        return post(qrCodePath, request, QrCodeResponse.class, "QR code generation");
    }

    private <T> T post(String path, Object body, Class<T> responseType, String operation) {
        try {
            log.info("Level 4 {} requestBody={}", operation, requestBody(body));
            String responseBody = restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(String.class);
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Level 4 returned an empty response for " + operation);
            }
            log.info("Level 4 {} responseBody={}", operation, responseBody);
            return readResponse(responseBody, responseType, operation);
        } catch (RestClientException exception) {
            logLevel4Failure(path, operation, body, exception);
            throw new IllegalStateException(
                    "Could not send " + operation + " to Level 4: " + level4ErrorMessage(exception),
                    exception);
        }
    }

    private <T> T readResponse(String responseBody, Class<T> responseType, String operation) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode payload = firstPresent(root, "result", "data");
            if (payload != null) {
                if (payload.isNull()) {
                    String message = root.path("message").asText("Level 4 returned an empty payload");
                    throw new IllegalStateException("Level 4 returned no payload for " + operation + ": " + message);
                }
                return objectMapper.treeToValue(payload, responseType);
            }
            return objectMapper.treeToValue(root, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not parse Level 4 response for " + operation, exception);
        }
    }

    private JsonNode firstPresent(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (root.has(fieldName)) {
                return root.get(fieldName);
            }
        }
        return null;
    }

    private void logLevel4Failure(String path, String operation, Object body, RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.error(
                    "Could not send {} to Level 4; baseUrl={}, path={}, requestBody={}, status={}, responseBody={}",
                    operation,
                    baseUrl,
                    path,
                    requestBody(body),
                    responseException.getStatusCode(),
                    responseException.getResponseBodyAsString(),
                    responseException);
            return;
        }

        log.error(
                "Could not send {} to Level 4; baseUrl={}, path={}, requestBody={}, exceptionType={}, message={}",
                operation,
                baseUrl,
                path,
                requestBody(body),
                exception.getClass().getName(),
                exception.getMessage(),
                exception);
    }

    private String requestBody(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            return String.valueOf(body);
        }
    }

    private String level4ErrorMessage(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            try {
                JsonNode root = objectMapper.readTree(responseException.getResponseBodyAsString());
                String message = root.path("message").asText();
                return message == null || message.isBlank() ? responseException.getStatusText() : message;
            } catch (JsonProcessingException ignored) {
                return responseException.getResponseBodyAsString();
            }
        }
        return exception.getMessage();
    }
}
