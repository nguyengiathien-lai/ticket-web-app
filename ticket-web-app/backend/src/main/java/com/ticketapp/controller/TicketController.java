package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.catalog.TicketTypeSyncRequest;
import com.ticketapp.dto.purchase.TicketPurchaseRequest;
import com.ticketapp.dto.purchase.TicketPurchaseResponse;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.dto.ticket.TicketTypeResponse;
import com.ticketapp.service.PurchaseService;
import com.ticketapp.service.TicketService;
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

    private final TicketService ticketService;
    private final PurchaseService purchaseService;

    public TicketController(
            TicketService ticketService, PurchaseService purchaseService) {
        this.ticketService = ticketService;
        this.purchaseService = purchaseService;
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<TicketTypeResponse>>> getTicketTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.getActiveTicketTypes(),
                "Ticket types retrieved successfully"));
    }

    @PostMapping("/types/cache")
    public ResponseEntity<ApiResponse<List<TicketTypeResponse>>> cacheTicketTypes(
            @Valid @RequestBody List<@Valid TicketTypeSyncRequest> request) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.cacheTicketTypes(request),
                "Ticket types cached successfully"));
    }

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<TicketPurchaseResponse>> purchaseTicket(
            @Valid @RequestBody TicketPurchaseRequest request) {
        TicketPurchaseResponse purchase = purchaseService.purchaseTicket(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(purchase, "Ticket purchased successfully"));
    }

}
