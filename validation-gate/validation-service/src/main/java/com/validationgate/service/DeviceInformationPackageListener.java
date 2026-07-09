package com.validationgate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.client.Level4Client;
import com.validationgate.dto.DeviceInformationPackageMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@Component
public class DeviceInformationPackageListener {

    private static final Logger log = LoggerFactory.getLogger(DeviceInformationPackageListener.class);

    private final ObjectMapper objectMapper;
    private final DeviceInformationPackageService packageService;
    private final Level4Client level4Client;

    public DeviceInformationPackageListener(
            ObjectMapper objectMapper,
            DeviceInformationPackageService packageService,
            Level4Client level4Client) {
        this.objectMapper = objectMapper;
        this.packageService = packageService;
        this.level4Client = level4Client;
    }

    @RabbitListener(queues = "#{@devicePackageQueueNames}")
    public void receive(Message message) {
        String packageJson = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received device information package message, routingKey='{}', payloadLength={}",
                message.getMessageProperties().getReceivedRoutingKey(), packageJson.length());
        DeviceInformationPackageMessage packageMessage;
        try {
            packageMessage = objectMapper.readValue(packageJson, DeviceInformationPackageMessage.class);
        } catch (JsonProcessingException exception) {
            log.error("Could not parse device information package JSON", exception);
            ackFailure(extractSyncId(packageJson), "Could not parse device information package JSON");
            return;
        }

        try {
            packageService.storePackage(packageMessage, deviceCode(message));
        } catch (RuntimeException exception) {
            log.error("Could not store device information package", exception);
            ackFailure(packageMessage.syncId(), exception.getMessage());
            return;
        }

        try {
            level4Client.ackControlPackageApply(packageMessage.syncId(), "APPLIED", null);
        } catch (RuntimeException exception) {
            log.error("Could not ack applied device information package; syncId={}", packageMessage.syncId(), exception);
            throw exception;
        }
        log.info("Stored device information package successfully");
    }

    private String deviceCode(Message message) {
        String queue = message.getMessageProperties().getConsumerQueue();
        if (queue == null || !queue.startsWith("device.") || queue.length() == "device.".length()) {
            throw new IllegalArgumentException("Could not resolve device code from consumer queue");
        }
        return queue.substring("device.".length());
    }

    private void ackFailure(Long syncId, String errorMessage) {
        try {
            level4Client.ackControlPackageApply(syncId, "FAILED", errorMessage);
        } catch (RuntimeException ackException) {
            log.error("Could not ack failed device information package apply; syncId={}", syncId, ackException);
        }
    }

    private Long extractSyncId(String packageJson) {
        try {
            JsonNode root = objectMapper.readTree(packageJson);
            JsonNode syncId = root.get("syncId");
            return syncId == null || syncId.isNull() || !syncId.canConvertToLong() ? null : syncId.asLong();
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
