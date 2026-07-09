package com.ticketapp.dto.order;

import com.ticketapp.entity.Order;
import com.ticketapp.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderNotificationResponse(
        String orderId,
        String orderCode,
        String category,
        String itemCode,
        BigDecimal totalAmount,
        String currency,
        String status,
        LocalDateTime orderedAt) {

    public static OrderNotificationResponse from(Order order) {
        boolean cardPurchase = order.getItems().stream()
                .anyMatch(item -> "PHYSICAL_CARD".equalsIgnoreCase(item.getItemType()));
        String itemCode = order.getItems().stream()
                .filter(item -> cardPurchase
                        ? "PHYSICAL_CARD".equalsIgnoreCase(item.getItemType())
                        : "TICKET".equalsIgnoreCase(item.getItemType()))
                .map(OrderItem::getItemCode)
                .findFirst()
                .orElse(null);

        return new OrderNotificationResponse(
                order.getExternalOrderId(),
                order.getOrderCode(),
                cardPurchase ? "CARD_PURCHASE" : "TICKET_PURCHASE",
                itemCode,
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getOrderedAt());
    }
}
