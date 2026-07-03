package com.validationgate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public DeviceInformationPackageListener(
            ObjectMapper objectMapper,
            DeviceInformationPackageService packageService) {
        this.objectMapper = objectMapper;
        this.packageService = packageService;
    }

    @RabbitListener(queues = "#{devicePackageQueue.name}")
    public void receive(Message message) {
        String packageJson = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received device information package message, routingKey='{}', payloadLength={}",
                message.getMessageProperties().getReceivedRoutingKey(), packageJson.length());
        try {
            DeviceInformationPackageMessage packageMessage =
                    objectMapper.readValue(packageJson, DeviceInformationPackageMessage.class);
            packageService.storePackage(packageMessage);
            log.info("Stored device information package successfully");
        } catch (JsonProcessingException exception) {
            log.error("Could not parse device information package JSON", exception);
            throw new IllegalArgumentException("Could not parse device information package JSON", exception);
        } catch (RuntimeException exception) {
            log.error("Could not store device information package", exception);
            throw exception;
        }
    }
}
