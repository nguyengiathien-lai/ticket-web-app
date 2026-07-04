package com.validationgate.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record DeviceInformationPackageMessage(
        Long syncId,
        String publishedAt,
        JsonNode deviceConfig,
        JsonNode stationContext,
        JsonNode mediaAccessRules) {
}
