package com.validationgate.controller;

import com.validationgate.dto.ValidationRecordRequest;
import com.validationgate.dto.ValidationRecordResponse;
import com.validationgate.service.GateValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate")
public class GateController {

    private final GateValidationService gateValidationService;

    public GateController(GateValidationService gateValidationService) {
        this.gateValidationService = gateValidationService;
    }

    @PostMapping("/validate-ticket")
    public ResponseEntity<ValidationRecordResponse> validateTicket(
            @Valid @RequestBody ValidationRecordRequest request) {
        return ResponseEntity.ok(gateValidationService.recordValidation(request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ValidationRecordResponse> handleDeliveryFailure(IllegalStateException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ValidationRecordResponse("Scan record was not received by the higher system"));
    }
}
