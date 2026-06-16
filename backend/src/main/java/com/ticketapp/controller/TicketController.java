package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.service.TicketRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRequestService ticketRequestService;

    public TicketController(TicketRequestService ticketRequestService) {
        this.ticketRequestService = ticketRequestService;
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<TicketResponse>> requestTicket(
            @Valid @RequestBody TicketRequest request) {
        TicketResponse ticket = ticketRequestService.requestTicket(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticket, "Ticket requested from external system and cached locally"));
    }

    @GetMapping("/{ticketCode}")
    public ResponseEntity<ApiResponse<TicketResponse>> getCachedTicketByCode(
            @PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketRequestService.getCachedTicketByCode(ticketCode),
                "Cached ticket retrieved"));
    }

    @GetMapping("/passenger/{passengerAccountId}")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getCachedTicketsForPassenger(
            @PathVariable String passengerAccountId) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketRequestService.getCachedTicketsForPassenger(passengerAccountId),
                "Cached tickets retrieved"));
    }
}
