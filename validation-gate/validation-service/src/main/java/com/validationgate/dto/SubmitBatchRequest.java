package com.validationgate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitBatchRequest {
    @NotNull
    @Size(min = 1, max = 500)
    @Valid
    private List<BatchTransactionItem> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchTransactionItem {
        @NotNull
        private String qrPayload;

        private String tapType;

        @NotNull
        private LocalDateTime occurredAt;
    }
}
