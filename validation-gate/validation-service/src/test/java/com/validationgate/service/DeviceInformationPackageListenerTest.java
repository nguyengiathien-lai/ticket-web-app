package com.validationgate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.client.Level4Client;
import com.validationgate.dto.DeviceInformationPackageMessage;
import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.SubmitBatchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class DeviceInformationPackageListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acksAppliedAfterStoringPackage() {
        StubPackageService packageService = new StubPackageService();
        CapturingLevel4Client level4Client = new CapturingLevel4Client();
        DeviceInformationPackageListener listener = new DeviceInformationPackageListener(
                objectMapper,
                packageService,
                level4Client);

        listener.receive(message("""
                {
                  "syncId": 42,
                  "publishedAt": "2026-06-25T00:00:00Z",
                  "deviceConfig": {},
                  "stationContext": {"stationCode": "station-1"},
                  "mediaAccessRules": {"cardStatusRules": []}
                }
                """));

        org.assertj.core.api.Assertions.assertThat(packageService.storedMessages).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(level4Client.acks)
                .containsExactly(new Ack(42L, "APPLIED", null));
    }

    @Test
    void acksFailedWhenPackageCannotBeStored() {
        StubPackageService packageService = new StubPackageService();
        packageService.failure = new IllegalArgumentException("wrong station");
        CapturingLevel4Client level4Client = new CapturingLevel4Client();
        DeviceInformationPackageListener listener = new DeviceInformationPackageListener(
                objectMapper,
                packageService,
                level4Client);

        listener.receive(message("""
                {
                  "syncId": 43,
                  "publishedAt": "2026-06-25T00:00:00Z",
                  "deviceConfig": {},
                  "stationContext": {"stationCode": "station-2"},
                  "mediaAccessRules": {"cardStatusRules": []}
                }
                """));

        org.assertj.core.api.Assertions.assertThat(level4Client.acks)
                .containsExactly(new Ack(43L, "FAILED", "wrong station"));
    }

    @Test
    void acksFailedWhenPackageCannotBeParsed() {
        StubPackageService packageService = new StubPackageService();
        CapturingLevel4Client level4Client = new CapturingLevel4Client();
        DeviceInformationPackageListener listener = new DeviceInformationPackageListener(
                objectMapper,
                packageService,
                level4Client);

        listener.receive(message("""
                {
                  "syncId": 44,
                  "publishedAt":
                }
                """));

        org.assertj.core.api.Assertions.assertThat(packageService.storedMessages).isEmpty();
        org.assertj.core.api.Assertions.assertThat(level4Client.acks)
                .containsExactly(new Ack(null, "FAILED", "Could not parse device information package JSON"));
    }

    private Message message(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    private static class StubPackageService extends DeviceInformationPackageService {
        private final List<DeviceInformationPackageMessage> storedMessages = new ArrayList<>();
        private RuntimeException failure;

        private StubPackageService() {
            super(null, null, null, null, null);
        }

        @Override
        public void storePackage(DeviceInformationPackageMessage message) {
            if (failure != null) {
                throw failure;
            }
            storedMessages.add(message);
        }
    }

    private static class CapturingLevel4Client implements Level4Client {
        private final List<Ack> acks = new ArrayList<>();

        @Override
        public SubmitBatchResponse sendBatch(SubmitBatchRequest request) {
            return null;
        }

        @Override
        public void ackControlPackageApply(Long syncId, String syncStatus, String errorMessage) {
            acks.add(new Ack(syncId, syncStatus, errorMessage));
        }
    }

    private record Ack(Long syncId, String syncStatus, String errorMessage) {
    }
}
