package com.validationgate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitBatchResponse {
    private int total;
    private int success;
    private int failed;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
