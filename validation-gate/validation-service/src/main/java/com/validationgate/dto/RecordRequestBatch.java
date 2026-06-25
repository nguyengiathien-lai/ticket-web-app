package com.validationgate.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RecordRequestBatch {
    private String batchId;
    private LocalDateTime generatedAt;
    private List<ExternalGateEventRequest> records;
}
