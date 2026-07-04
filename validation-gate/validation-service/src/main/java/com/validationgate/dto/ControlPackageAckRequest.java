package com.validationgate.dto;

public record ControlPackageAckRequest(
        String syncStatus,
        String errorMessage) {
}
