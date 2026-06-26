package com.validationgate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.dto.DeviceInformationPackageMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DeviceInformationPackageListener {

    private final ObjectMapper objectMapper;
    private final DeviceInformationPackageService packageService;

    public DeviceInformationPackageListener(
            ObjectMapper objectMapper,
            DeviceInformationPackageService packageService) {
        this.objectMapper = objectMapper;
        this.packageService = packageService;
    }

    @RabbitListener(queues = "#{devicePackageQueue.name}")
    public void receive(String packageJson) {
        try {
            DeviceInformationPackageMessage message =
                    objectMapper.readValue(packageJson, DeviceInformationPackageMessage.class);
            packageService.storePackage(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not parse device information package JSON", exception);
        }
    }
}
