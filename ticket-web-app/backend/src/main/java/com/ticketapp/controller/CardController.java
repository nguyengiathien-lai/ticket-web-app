package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.catalog.CardTypeSyncRequest;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.service.CardService;
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
    private final CardService cardService;

    public CardController(PurchaseService purchaseService, CardService cardService) {
        this.purchaseService = purchaseService;
        this.cardService = cardService;
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<CardTypeResponse>>> getCardTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getActiveCardTypes(),
                "Card types retrieved successfully"));
    }

    @PostMapping("/types/cache")
    public ResponseEntity<ApiResponse<List<CardTypeResponse>>> cacheCardTypes(
            @Valid @RequestBody List<@Valid CardTypeSyncRequest> request) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.cacheCardTypes(request),
                "Card types cached successfully"));
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
