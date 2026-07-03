package com.ticketapp.controller;

import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/issuance")
public class IssuanceController {

    private final PurchaseService purchaseService;

    public IssuanceController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/cards")
    public ResponseEntity<CardPurchaseResponse> issueCard(@Valid @RequestBody CardPurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.purchaseCard(request));
    }
}
