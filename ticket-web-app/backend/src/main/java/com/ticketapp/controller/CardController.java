package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.fare.FarePackageResponse;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.service.CardService;
import com.ticketapp.service.PurchaseService;
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
@RequestMapping("/cards")
@Tag(name = "Cards", description = "Card fare packages and purchase APIs")
public class CardController {

    private final PurchaseService purchaseService;
    private final CardService cardService;

    public CardController(PurchaseService purchaseService, CardService cardService) {
        this.purchaseService = purchaseService;
        this.cardService = cardService;
    }

    @GetMapping("/fare-packages")
    public ResponseEntity<ApiResponse<List<FarePackageResponse>>> getFarePackages() {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getActiveFarePackages(),
                "Fare packages retrieved successfully"));
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
