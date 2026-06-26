package com.validationgate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.client.Level4Client;
import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.SubmitBatchResponse;
import com.validationgate.dto.TapEventType;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.entity.DeviceConfigPackage;
import com.validationgate.entity.MediaAccessRulesPackage;
import com.validationgate.entity.TapEvent;
import com.validationgate.repository.DeviceConfigPackageRepository;
import com.validationgate.repository.MediaAccessRulesPackageRepository;
import com.validationgate.repository.TapEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GateValidationServiceTest {

    private static final Duration SENT_EVENT_RETENTION = Duration.ofDays(7);
    private static final String SECRET = "validation-secret";
    private static final String ENCODED_SECRET = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(SECRET.getBytes(StandardCharsets.UTF_8));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validatesQrPayloadWithDeviceConfigKeyAndStoresTheScanRecordForBatchDelivery() {
        Level4Client client = mock(Level4Client.class);
        TapEventRepository repository = mock(TapEventRepository.class);
        DeviceConfigPackageRepository deviceConfigRepository = mock(DeviceConfigPackageRepository.class);
        MediaAccessRulesPackageRepository mediaAccessRulesRepository = mock(MediaAccessRulesPackageRepository.class);
        when(deviceConfigRepository.findByStationCode("station-1")).thenReturn(Optional.of(deviceConfig()));
        when(mediaAccessRulesRepository.findByStationCode("station-1")).thenReturn(Optional.of(mediaRules("other-card")));
        GateValidationService service = service(client, repository, deviceConfigRepository, mediaAccessRulesRepository);
        ValidationRequest request = request("ticket-42");
        LocalDateTime beforeRecord = LocalDateTime.now();

        Boolean valid = service.validateTicket(request);
        LocalDateTime afterRecord = LocalDateTime.now();

        ArgumentCaptor<TapEvent> captor = ArgumentCaptor.forClass(TapEvent.class);
        verify(repository).save(captor.capture());
        assertThat(valid).isTrue();
        assertThat(captor.getValue().getTicketId()).isEqualTo("ticket-42");
        assertThat(captor.getValue().getGateId()).isEqualTo("device-1");
        assertThat(captor.getValue().getStationId()).isEqualTo("station-1");
        assertThat(captor.getValue().getEventType()).isEqualTo(TapEventType.CHECK_IN);
        assertThat(captor.getValue().getRecordedAt()).isBetween(beforeRecord, afterRecord);
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getEventId()).isNotBlank();
        verify(client, never()).sendBatch(any(SubmitBatchRequest.class));
    }

    @Test
    void rejectsInvalidQrPayloadWhenCalledOutsideTheController() {
        GateValidationService service = service(mock(Level4Client.class), mock(TapEventRepository.class),
                mock(DeviceConfigPackageRepository.class), mock(MediaAccessRulesPackageRepository.class));
        ValidationRequest request = new ValidationRequest();
        request.setQrPayload("invalid");
        request.setDeviceCode("device-1");
        request.setStationCode("station-1");
        request.setEventType(TapEventType.CHECK_IN);

        assertThatThrownBy(() -> service.recordTapEvent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("QR payload is invalid");
    }

    @Test
    void expiredQrPayloadIsDeniedAndNotStored() {
        TapEventRepository repository = mock(TapEventRepository.class);
        DeviceConfigPackageRepository deviceConfigRepository = mock(DeviceConfigPackageRepository.class);
        MediaAccessRulesPackageRepository mediaAccessRulesRepository = mock(MediaAccessRulesPackageRepository.class);
        when(deviceConfigRepository.findByStationCode("station-1")).thenReturn(Optional.of(deviceConfig()));
        GateValidationService service = service(mock(Level4Client.class), repository,
                deviceConfigRepository, mediaAccessRulesRepository);
        ValidationRequest request = new ValidationRequest();
        request.setQrPayload(signedPayload("card-42", "ticket-42", 1));
        request.setDeviceCode("device-1");
        request.setStationCode("station-1");
        request.setEventType(TapEventType.CHECK_IN);

        Boolean valid = service.validateTicket(request);

        assertThat(valid).isFalse();
        verify(repository, never()).save(any(TapEvent.class));
    }

    @Test
    void deniesQrPayloadWhenSignatureDoesNotMatchDeviceConfigKey() {
        TapEventRepository repository = mock(TapEventRepository.class);
        DeviceConfigPackageRepository deviceConfigRepository = mock(DeviceConfigPackageRepository.class);
        MediaAccessRulesPackageRepository mediaAccessRulesRepository = mock(MediaAccessRulesPackageRepository.class);
        when(deviceConfigRepository.findByStationCode("station-1")).thenReturn(Optional.of(deviceConfig()));
        GateValidationService service = service(mock(Level4Client.class), repository,
                deviceConfigRepository, mediaAccessRulesRepository);
        ValidationRequest request = request("ticket-42");
        request.setQrPayload(request.getQrPayload().replace("hmac=", "hmac=bad"));

        Boolean valid = service.validateTicket(request);

        assertThat(valid).isFalse();
        verify(repository, never()).save(any(TapEvent.class));
    }

    @Test
    void deniesQrPayloadWhenCardIsBlacklistedInMediaAccessRules() {
        TapEventRepository repository = mock(TapEventRepository.class);
        DeviceConfigPackageRepository deviceConfigRepository = mock(DeviceConfigPackageRepository.class);
        MediaAccessRulesPackageRepository mediaAccessRulesRepository = mock(MediaAccessRulesPackageRepository.class);
        when(deviceConfigRepository.findByStationCode("station-1")).thenReturn(Optional.of(deviceConfig()));
        when(mediaAccessRulesRepository.findByStationCode("station-1")).thenReturn(Optional.of(mediaRules("card-42")));
        GateValidationService service = service(mock(Level4Client.class), repository,
                deviceConfigRepository, mediaAccessRulesRepository);
        ValidationRequest request = request("ticket-42");

        Boolean valid = service.validateTicket(request);

        assertThat(valid).isFalse();
        verify(repository, never()).save(any(TapEvent.class));
    }

    @Test
    void sendsPendingRecordsAsABatchAndMarksThemSent() {
        Level4Client client = mock(Level4Client.class);
        TapEventRepository repository = mock(TapEventRepository.class);
        TapEvent event = event("event-1");
        when(repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                anyCollection(), any(Pageable.class))).thenReturn(List.of(event));
        when(client.sendBatch(any())).thenReturn(new SubmitBatchResponse("Batch received"));
        GateValidationService service = service(client, repository,
                mock(DeviceConfigPackageRepository.class), mock(MediaAccessRulesPackageRepository.class), 50);

        service.flushValidationBatch();

        ArgumentCaptor<SubmitBatchRequest> batchCaptor = ArgumentCaptor.forClass(SubmitBatchRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByDeliveryStatusInOrderByRecordedAtAsc(anyCollection(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);

        verify(client).sendBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getBatchId()).isNotBlank();
        assertThat(batchCaptor.getValue().getGeneratedAt()).isNotNull();
        assertThat(batchCaptor.getValue().getRecords()).containsExactly(event);
        assertThat(event.getDeliveryStatus()).isEqualTo("SENT");
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getDeliveryError()).isNull();
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void marksTheBatchFailedWhenLevel4RejectsIt() {
        Level4Client client = mock(Level4Client.class);
        TapEventRepository repository = mock(TapEventRepository.class);
        TapEvent event = event("event-1");
        when(repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                anyCollection(), any(Pageable.class))).thenReturn(List.of(event));
        when(client.sendBatch(any())).thenThrow(new IllegalStateException("Level 4 unavailable"));
        GateValidationService service = service(client, repository,
                mock(DeviceConfigPackageRepository.class), mock(MediaAccessRulesPackageRepository.class), 50);

        service.flushValidationBatch();

        assertThat(event.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(event.getSentAt()).isNull();
        assertThat(event.getDeliveryError()).isEqualTo("Level 4 unavailable");
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void deletesSentRecordsAfterTheRetentionWindow() {
        TapEventRepository repository = mock(TapEventRepository.class);
        GateValidationService service = new GateValidationService(
                mock(Level4Client.class),
                repository,
                mock(DeviceConfigPackageRepository.class),
                mock(MediaAccessRulesPackageRepository.class),
                objectMapper,
                100,
                Duration.ofHours(2));
        LocalDateTime beforeCleanup = LocalDateTime.now().minusHours(2);

        service.deleteExpiredSentEvents();

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByDeliveryStatusAndSentAtBefore(
                org.mockito.ArgumentMatchers.eq("SENT"), thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isAfterOrEqualTo(beforeCleanup);
        assertThat(thresholdCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now().minusHours(2));
    }

    private ValidationRequest request(String ticketId) {
        ValidationRequest request = new ValidationRequest();
        long expiresAt = System.currentTimeMillis() / 1000 + 3600;
        request.setQrPayload(signedPayload("card-42", ticketId, expiresAt));
        request.setDeviceCode("device-1");
        request.setStationCode("station-1");
        request.setEventType(TapEventType.CHECK_IN);
        return request;
    }

    private GateValidationService service(
            Level4Client client,
            TapEventRepository tapEventRepository,
            DeviceConfigPackageRepository deviceConfigRepository,
            MediaAccessRulesPackageRepository mediaAccessRulesRepository) {
        return service(client, tapEventRepository, deviceConfigRepository, mediaAccessRulesRepository, 100);
    }

    private GateValidationService service(
            Level4Client client,
            TapEventRepository tapEventRepository,
            DeviceConfigPackageRepository deviceConfigRepository,
            MediaAccessRulesPackageRepository mediaAccessRulesRepository,
            int batchSize) {
        return new GateValidationService(
                client,
                tapEventRepository,
                deviceConfigRepository,
                mediaAccessRulesRepository,
                objectMapper,
                batchSize,
                SENT_EVENT_RETENTION);
    }

    private DeviceConfigPackage deviceConfig() {
        DeviceConfigPackage entity = new DeviceConfigPackage();
        entity.setDeviceCode("device-1");
        entity.setStationCode("station-1");
        entity.setPayloadJson("""
                {"qrVerificationAlgorithm":"HMAC_SHA256","qrVerificationKey":"%s","maxClockDriftSeconds":60,"allowOfflineValidation":true}
                """.formatted(ENCODED_SECRET));
        return entity;
    }

    private MediaAccessRulesPackage mediaRules(String cardId) {
        MediaAccessRulesPackage entity = new MediaAccessRulesPackage();
        entity.setDeviceCode("device-1");
        entity.setStationCode("station-1");
        entity.setPayloadJson("""
                {"cardStatusRules":[{"cardId":"%s","status":"BLACKLISTED"}]}
                """.formatted(cardId));
        return entity;
    }

    private String signedPayload(String cardId, String ticketId, long expiresAt) {
        String dataToSign = "AFCQR:v1:" + cardId + ":" + ticketId + ":exp=" + expiresAt;
        return dataToSign + ":hmac=" + hmacSha256Base64Url(SECRET, dataToSign);
    }

    private String hmacSha256Base64Url(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private TapEvent event(String eventId) {
        TapEvent event = new TapEvent();
        event.setEventId(eventId);
        event.setTicketId("ticket-42");
        event.setGateId("gate-1");
        event.setStationId("station-1");
        event.setEventType(TapEventType.CHECK_OUT);
        event.setRecordedAt(LocalDateTime.of(2026, 6, 21, 15, 30));
        event.setDeliveryStatus("PENDING");
        return event;
    }
}
