package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.gate.GateValidationRequest;
import com.ticketapp.dto.gate.GateValidationResponse;
import com.ticketapp.service.GateValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<GateValidationResponse>> validateTicket(
            @Valid @RequestBody GateValidationRequest request) {
        GateValidationResponse response = gateValidationService.validateTicket(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}
