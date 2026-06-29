package com.validationgate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.SubmitBatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExternalLevel4Client implements Level4Client {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String scanRecordBatchPath;
    private final String deviceCode;
    private final String deviceSecret;
    private final boolean mockEnabled;

    public ExternalLevel4Client(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.level4.base-url:}") String baseUrl,
            @Value("${app.level4.scan-record-batch-path:/vdt/transaction/submit-batch}") String scanRecordBatchPath,
            @Value("${app.device.code:}") String deviceCode,
            @Value("${app.device.secret:}") String deviceSecret,
            @Value("${app.level4.mock-enabled:false}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.scanRecordBatchPath = scanRecordBatchPath;
        this.deviceCode = deviceCode;
        this.deviceSecret = deviceSecret;
        this.mockEnabled = mockEnabled;
    }

    @Override
    public SubmitBatchResponse sendBatch(SubmitBatchRequest request) {
        log.info("Sending scan record batch to Level 4; mockEnabled={}, baseUrl={}, path={}, deviceCode={}, deviceSecretPresent={}",
                mockEnabled, baseUrl, scanRecordBatchPath, deviceCode, hasText(deviceSecret));
        if (mockEnabled) {
            log.info("Mock enabled, returning a mock response for scan record batch");
            int total = request == null || request.getTransactions() == null ? 0 : request.getTransactions().size();
            return SubmitBatchResponse.builder()
                    .total(total)
                    .success(total)
                    .failed(0)
                    .build();
        }
        return post(scanRecordBatchPath, request, SubmitBatchResponse.class, "scan record batch");
    }

    private <T> T post(
            String path,
            Object body,
            Class<T> responseType,
            String operation) {
        requireDeviceCredentials();
        try {
            log.info("Level 4 {} requestBody={}", operation, requestBody(body));
            String responseBody = restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .header("X-Device-Code", deviceCode)
                    .header("X-Device-Secret", deviceSecret)
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
            if (root.has("result")) {
                JsonNode result = root.get("result");
                if (result == null || result.isNull()) {
                    String message = root.path("message").asText("Level 4 returned an empty result");
                    throw new IllegalStateException("Level 4 returned no result for " + operation + ": " + message);
                }
                return objectMapper.treeToValue(result, responseType);
            }
            return objectMapper.treeToValue(root, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not parse Level 4 response for " + operation, exception);
        }
    }

    private void logLevel4Failure(
            String path,
            String operation,
            Object body,
            RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.error(
                    "Could not send {} to Level 4; baseUrl={}, path={}, deviceCode={}, deviceSecretPresent={}, requestBody={}, status={}, responseBody={}",
                    operation,
                    baseUrl,
                    path,
                    deviceCode,
                    hasText(deviceSecret),
                    requestBody(body),
                    responseException.getStatusCode(),
                    responseException.getResponseBodyAsString(),
                    responseException);
            return;
        }

        log.error(
                "Could not send {} to Level 4; baseUrl={}, path={}, deviceCode={}, deviceSecretPresent={}, requestBody={}, exceptionType={}, message={}",
                operation,
                baseUrl,
                path,
                deviceCode,
                hasText(deviceSecret),
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
                return hasText(message) ? message : responseException.getStatusText();
            } catch (JsonProcessingException ignored) {
                return responseException.getResponseBodyAsString();
            }
        }
        return exception.getMessage();
    }

    private void requireDeviceCredentials() {
        if (!hasText(deviceCode) || !hasText(deviceSecret)) {
            throw new IllegalStateException(
                    "Level 4 device credentials are not configured; set DEVICE_CODE and DEVICE_SECRET");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
