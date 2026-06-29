package com.validationgate.controller;

import com.validationgate.dto.ValidationRequest;
import com.validationgate.dto.SubmitBatchResponse;
import com.validationgate.service.GateValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gate")
public class GateController {

    private final GateValidationService gateValidationService;

    public GateController(GateValidationService gateValidationService) {
        this.gateValidationService = gateValidationService;
    }

    // @PostMapping("/validate-ticket")
    // public ResponseEntity<SubmitBatchResponse> validateTicket(
    //         @Valid @RequestBody ValidationRequest request) {
    //     return ResponseEntity.ok(gateValidationService.validateTicket(request));
    // }
    @PostMapping("/validate-ticket")
    public ResponseEntity<Boolean> validateTicket(
            @Valid @RequestBody ValidationRequest request) {
        return ResponseEntity.ok(gateValidationService.validateTicket(request));
    }

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
