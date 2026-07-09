package com.validationgate.controller;

import com.validationgate.config.DeviceProperties;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.dto.SubmitBatchResponse;
import com.validationgate.service.GateValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gate")
@Tag(name = "Gate Validation", description = "APIs for validating tickets and submitting scan batches")
public class GateController {

    private final GateValidationService gateValidationService;
    private final DeviceProperties deviceProperties;

    public GateController(GateValidationService gateValidationService, DeviceProperties deviceProperties) {
        this.gateValidationService = gateValidationService;
        this.deviceProperties = deviceProperties;
    }

    @Operation(summary = "Get configured scanner station and device options")
    @GetMapping("/scanner-options")
    public List<DeviceProperties.DeviceRegistration> scannerOptions() {
        return deviceProperties.devices();
    }

    @Operation(
            summary = "Validate a ticket",
            description = "Validates a scanned QR ticket for the configured gate and station")
    @PostMapping("/validate-ticket")
    public ResponseEntity<Boolean> validateTicket(
            @Valid @RequestBody ValidationRequest request) {
        return ResponseEntity.ok(gateValidationService.validateTicket(request));
    }

    @Operation(
            summary = "Submit the pending validation batch",
            description = "Flushes pending scan records to the higher-level system")
    @PostMapping("/submit-batch")
    public ResponseEntity<SubmitBatchResponse> flushValidationBatch() {
        return ResponseEntity.ok(gateValidationService.flushValidationBatch());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<SubmitBatchResponse> handleDeliveryFailure(IllegalStateException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(SubmitBatchResponse.builder()
                        .failed(1)
                        .errors(List.of("Scan record was not received by the higher system"))
                        .build());
    }
}
