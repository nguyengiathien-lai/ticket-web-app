package com.validationgate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationRequest {

    @NotBlank
    private String qrPayload;

    @NotBlank
    private String deviceCode;

    @NotBlank
    private String stationCode;

    @NotNull
    private TapEventType eventType;
}
