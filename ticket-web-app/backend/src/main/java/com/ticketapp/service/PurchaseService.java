package com.ticketapp.service;

import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.dto.purchase.TicketPurchaseRequest;
import com.ticketapp.dto.purchase.TicketPurchaseResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.FarePackage;
import com.ticketapp.entity.Order;
import com.ticketapp.entity.OrderItem;
import com.ticketapp.entity.Payment;
import com.ticketapp.entity.PhysicalCard;
import com.ticketapp.repository.OrderRepository;
import com.ticketapp.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseService {

    private final AccountService accountService;
    private final TicketService ticketService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CardService cardService;
    private final Level4Client level4Client;
    private final Level5Client level5Client;

    public PurchaseService(
            AccountService accountService,
            TicketService ticketService,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CardService cardService,
            Level4Client level4Client,
            Level5Client level5Client) {
        this.accountService = accountService;
        this.ticketService = ticketService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.cardService = cardService;
        this.level4Client = level4Client;
        this.level5Client = level5Client;
    }

    @Transactional
    public TicketPurchaseResponse purchaseTicket(TicketPurchaseRequest request) {
        requirePurchasableAccount(request.getUserId());
        FarePackage farePackage = ticketService.requireActiveFarePackage(request.getPackageId());
        LocalDateTime now = LocalDateTime.now();

        Order order = createOrder(
                request.getUserId(),
                farePackage.getPrice(),
                farePackage.getCurrency(),
                "COMPLETED",
                now,
                List.of(createOrderItem(
                        "TICKET",
                        farePackage.getCode(),
                        farePackage.getName(),
                        1,
                        farePackage.getPrice())));
        Payment payment = createCompletedPayment(order, request.getUserId(), request.getPaymentMethod(), now);

        TicketRequest ticketRequest = new TicketRequest();
        ticketRequest.setPassengerAccountId(request.getUserId());
        ticketRequest.setTicketTypeCode(request.getPackageId());
        ticketRequest.setIdempotencyKey(order.getExternalOrderId());
        ExternalTicketResponse externalTicket = level5Client.purchaseTicket(ExternalTicketRequest.builder()
                .passengerAccountId(request.getUserId())
                .ticketTypeCode(farePackage.getCode())
                .idempotencyKey(order.getExternalOrderId())
                .requestSource("TICKET_WEB_APP")
                .orderId(order.getExternalOrderId())
                .paymentId(payment.getExternalPaymentId())
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build());
        TicketResponse ticket = ticketService.cacheExternalTicket(ticketRequest, externalTicket);
        QrCodeResponse qrCode = level4Client.generateQrCode(new QrCodeRequest(ticket.getExternalTicketId()));

        return TicketPurchaseResponse.builder()
                .ticketId(ticket.getExternalTicketId())
                .orderId(order.getExternalOrderId())
                .userId(request.getUserId())
                .packageId(farePackage.getCode())
                .origin("Central Station")
                .destination("Airport Terminal")
                .totalPrice(order.getTotalAmount())
                .currency(order.getCurrency())
                .status("confirmed")
                .qrCode(qrCode.getQrCode())
                .confirmationNumber(order.getOrderCode())
                .paymentId(payment.getExternalPaymentId())
                .paymentStatus(payment.getStatus())
                .purchasedAt(now)
                .build();
    }

    @Transactional
    public CardPurchaseResponse purchaseCard(CardPurchaseRequest request) {
        requirePurchasableAccount(request.getUserId());
        FarePackage farePackage = cardService.requireActiveFarePackage(request.getPackageId());
        LocalDateTime now = LocalDateTime.now();

        Order order = createOrder(
                request.getUserId(),
                farePackage.getPrice(),
                farePackage.getCurrency(),
                "PROCESSING",
                now,
                List.of(createOrderItem(
                        "PHYSICAL_CARD",
                        farePackage.getCode(),
                        farePackage.getName(),
                        1,
                        farePackage.getPrice())));
        Payment payment = createCompletedPayment(order, request.getUserId(), request.getPaymentMethod(), now);

        ExternalCardResponse externalCard = level5Client.purchaseCard(ExternalCardRequest.builder()
                .passengerAccountId(request.getUserId())
                .cardTypeCode(farePackage.getCode())
                .orderId(order.getExternalOrderId())
                .requestSource("TICKET_WEB_APP")
                .paymentId(payment.getExternalPaymentId())
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .deliveryAddress(request.getDeliveryAddress())
                .build());
        requireCompleteCard(externalCard);

        PhysicalCard card = new PhysicalCard();
        card.setExternalCardId(externalCard.getExternalCardId());
        card.setPassengerAccountId(request.getUserId());
        card.setCardUid(coalesce(externalCard.getCardUid(), externalCard.getExternalCardId()));
        card.setMaskedCardNumber(externalCard.getMaskedCardNumber());
        card.setStatus(coalesce(externalCard.getStatus(), "INACTIVE"));
        card.setIssuedAt(externalCard.getIssuedAt() == null ? now : externalCard.getIssuedAt());
        card.setExpiredAt(externalCard.getExpiresAt());
        card.setCachedAt(now);
        card.setExpiresAt(externalCard.getExpiresAt());
        PhysicalCard savedCard = cardService.cacheCard(card);

        return CardPurchaseResponse.builder()
                .orderId(order.getExternalOrderId())
                .userId(request.getUserId())
                .cardId(savedCard.getExternalCardId())
                .cardUid(savedCard.getCardUid())
                .maskedCardNumber(savedCard.getMaskedCardNumber())
                .packageId(farePackage.getCode())
                .status("in delivery")
                .deliveryAddress(request.getDeliveryAddress())
                .estimatedDelivery(LocalDate.now().plusDays(3))
                .totalPrice(order.getTotalAmount())
                .currency(order.getCurrency())
                .paymentId(payment.getExternalPaymentId())
                .paymentStatus(payment.getStatus())
                .createdAt(now)
                .build();
    }

    private void requirePurchasableAccount(String userId) {
        accountService.findById(userId)
                .filter(account -> account.getIsActive() && account.getIsEmailVerified())
                .orElseThrow(() -> new IllegalArgumentException("Passenger account not found, inactive, or unverified"));
    }

    private Order createOrder(
            String userId,
            BigDecimal totalAmount,
            String currency,
            String status,
            LocalDateTime now,
            List<OrderItem> items) {
        Order order = new Order();
        order.setExternalOrderId("order_" + shortToken());
        order.setPassengerAccountId(userId);
        order.setOrderCode("CONF" + shortToken().toUpperCase());
        order.setTotalAmount(totalAmount);
        order.setCurrency(currency);
        order.setStatus(status);
        order.setOrderedAt(now);
        order.setPaidAt(now);
        order.setItems(items);
        order.setCachedAt(now);
        order.setExpiresAt(now.plusDays(365));
        return orderRepository.save(order);
    }

    private OrderItem createOrderItem(String itemType, String itemCode, String itemName, int quantity, BigDecimal price) {
        return new OrderItem(
                "item_" + shortToken(),
                itemType,
                itemCode,
                itemName,
                quantity,
                price,
                price.multiply(BigDecimal.valueOf(quantity)));
    }

    private Payment createCompletedPayment(Order order, String userId, String paymentMethod, LocalDateTime now) {
        Payment payment = new Payment();
        payment.setExternalPaymentId("pay_" + shortToken());
        payment.setExternalOrderId(order.getExternalOrderId());
        payment.setPassengerAccountId(userId);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionCode("TXN" + shortToken().toUpperCase());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        payment.setStatus("COMPLETED");
        payment.setPaidAt(now);
        payment.setCachedAt(now);
        payment.setExpiresAt(now.plusDays(365));
        return paymentRepository.save(payment);
    }

    private void requireCompleteCard(ExternalCardResponse card) {
        if (card == null || card.getExternalCardId() == null || card.getExternalCardId().isBlank()) {
            throw new IllegalStateException("Level 5 returned an incomplete card");
        }
    }

    private String coalesce(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String shortToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

}
