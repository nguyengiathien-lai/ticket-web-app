package com.validationgate.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import com.validationgate.entity.TapEvent;

@Getter
@Builder
public class SubmitBatchRequest {
    private String batchId;
    private LocalDateTime generatedAt;
    private List<TapEvent> records;
}
