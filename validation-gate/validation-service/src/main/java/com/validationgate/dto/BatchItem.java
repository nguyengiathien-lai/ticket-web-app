package com.validationgate.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
public class BatchItem {
    private String qrPayload;
    private TapEventType eventType;
    private LocalDateTime recordedAt;
}
