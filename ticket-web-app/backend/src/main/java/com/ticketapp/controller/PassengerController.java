package com.ticketapp.controller;

import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalDiscountResponse;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.external.ExternalPassengerRouteResponse;
import com.ticketapp.dto.external.ExternalPassengerStationResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PassengerController {

    private final Level5Client level5Client;

    public PassengerController(Level5Client level5Client) {
        this.level5Client = level5Client;
    }

    @GetMapping("/passenger/stations")
    public ResponseEntity<ApiResponse<List<ExternalPassengerStationResponse>>> getStations() {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getStations(),
                "Passenger stations retrieved successfully"));
    }

    @GetMapping("/passenger/routes")
    public ResponseEntity<ApiResponse<List<ExternalPassengerRouteResponse>>> getRoutes() {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getRoutes(),
                "Passenger routes retrieved successfully"));
    }

    @GetMapping("/passengers/{userId}/cards")
    public ResponseEntity<ApiResponse<List<ExternalCardHistoryResponse>>> getPassengerCards(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getCards(userId),
                "Passenger cards retrieved successfully"));
    }

    @GetMapping("/passengers/{userId}/tickets")
    public ResponseEntity<ApiResponse<List<ExternalTicketHistoryResponse>>> getPassengerTickets(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getTickets(userId),
                "Passenger tickets retrieved successfully"));
    }

    @GetMapping("/passengers/{userId}/trips")
    public ResponseEntity<ApiResponse<List<ExternalTravelHistoryResponse>>> getPassengerTrips(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getTravelHistory(userId),
                "Passenger trips retrieved successfully"));
    }

    @GetMapping("/passenger/fare/prices")
    public ResponseEntity<ApiResponse<List<ExternalFarePriceResponse>>> getFarePrices() {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getFarePrices(),
                "Fare prices retrieved successfully"));
    }

    @GetMapping("/passenger/fare/discounts")
    public ResponseEntity<ApiResponse<List<ExternalDiscountResponse>>> getFareDiscounts() {
        return ResponseEntity.ok(ApiResponse.success(
                level5Client.getFareDiscounts(),
                "Fare discounts retrieved successfully"));
    }
}
