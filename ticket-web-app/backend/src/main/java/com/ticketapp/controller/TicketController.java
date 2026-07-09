package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.fare.FarePackageResponse;
import com.ticketapp.dto.purchase.TicketPurchaseRequest;
// import com.ticketapp.dto.purchase.PassTicketPurchaseRequest;
// import com.ticketapp.dto.purchase.SingleTripTicketPurchaseRequest;
import com.ticketapp.dto.purchase.TicketPurchaseResponse;
import com.ticketapp.service.PurchaseService;
import com.ticketapp.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "Ticket fare packages and purchase APIs")
public class TicketController {

    private final TicketService ticketService;
    private final PurchaseService purchaseService;

    public TicketController(
            TicketService ticketService, PurchaseService purchaseService) {
        this.ticketService = ticketService;
        this.purchaseService = purchaseService;
    }

    @GetMapping("/fare-packages")
    public ResponseEntity<ApiResponse<List<FarePackageResponse>>> getFarePackages() {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.getActiveFarePackages(),
                "Fare packages retrieved successfully"));
    }

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<TicketPurchaseResponse>> purchaseTicket(
            @Valid @RequestBody TicketPurchaseRequest request) {
        TicketPurchaseResponse purchase = purchaseService.purchaseTicket(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(purchase, "Ticket purchased successfully"));
    }

    // @PostMapping("/single-trip")
    // public ResponseEntity<ApiResponse<TicketPurchaseResponse>> purchaseSingleTripTicket(
    //         @Valid @RequestBody SingleTripTicketPurchaseRequest request) {
    //     TicketPurchaseResponse purchase = purchaseService.purchaseSingleTripTicket(request);

    //     return ResponseEntity
    //             .status(HttpStatus.CREATED)
    //             .body(ApiResponse.success(purchase, "Single-trip ticket purchased successfully"));
    // }

    // @PostMapping("/pass")
    // public ResponseEntity<ApiResponse<TicketPurchaseResponse>> purchaseMonthlyPassTicket(
    //         @Valid @RequestBody PassTicketPurchaseRequest request) {
    //     TicketPurchaseResponse purchase = purchaseService.purchaseMonthlyPassTicket(request);

    //     return ResponseEntity
    //             .status(HttpStatus.CREATED)
    //             .body(ApiResponse.success(purchase, "Monthly pass purchased successfully"));
    // }

}
