package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.service.PurchaseService;
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
@RequestMapping("/cards")
public class CardController {

    private final PurchaseService purchaseService;

    public CardController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<CardTypeResponse>>> getCardTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseService.getCardTypes(),
                "Card types retrieved successfully"));
    }

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<CardPurchaseResponse>> purchaseCard(
            @Valid @RequestBody CardPurchaseRequest request) {
        CardPurchaseResponse purchase = purchaseService.purchaseCard(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(purchase, "Card purchased successfully"));
    }
}
