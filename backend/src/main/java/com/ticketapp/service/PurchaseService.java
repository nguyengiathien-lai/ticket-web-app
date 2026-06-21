package com.ticketapp.service;

import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.PurchaseActivityRequest;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.dto.purchase.TicketPurchaseRequest;
import com.ticketapp.dto.purchase.TicketPurchaseResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.CardType;
import com.ticketapp.entity.Order;
import com.ticketapp.entity.OrderItem;
import com.ticketapp.entity.Payment;
import com.ticketapp.entity.PhysicalCard;
import com.ticketapp.entity.TicketType;
import com.ticketapp.repository.OrderRepository;
import com.ticketapp.repository.PaymentRepository;
import com.ticketapp.repository.PhysicalCardRepository;
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
    private final CatalogService catalogService;
    private final TicketRequestService ticketRequestService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PhysicalCardRepository physicalCardRepository;
    private final Level4Client level4Client;
    private final Level5Client level5Client;

    public PurchaseService(
            AccountService accountService,
            CatalogService catalogService,
            TicketRequestService ticketRequestService,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PhysicalCardRepository physicalCardRepository,
            Level4Client level4Client,
            Level5Client level5Client) {
        this.accountService = accountService;
        this.catalogService = catalogService;
        this.ticketRequestService = ticketRequestService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.physicalCardRepository = physicalCardRepository;
        this.level4Client = level4Client;
        this.level5Client = level5Client;
    }

    @Transactional
    public TicketPurchaseResponse purchaseTicket(TicketPurchaseRequest request) {
        requirePurchasableAccount(request.getUserId());
        TicketType ticketType = catalogService.requireActiveTicketType(request.getTicketType());
        LocalDateTime now = LocalDateTime.now();

        Order order = createOrder(
                request.getUserId(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                "COMPLETED",
                now,
                List.of(createOrderItem(
                        "TICKET",
                        ticketType.getCode(),
                        ticketType.getName(),
                        1,
                        ticketType.getPrice())));
        Payment payment = createCompletedPayment(order, request.getUserId(), request.getPaymentMethod(), now);

        TicketRequest ticketRequest = new TicketRequest();
        ticketRequest.setPassengerAccountId(request.getUserId());
        ticketRequest.setTicketTypeCode(request.getTicketType());
        ticketRequest.setIdempotencyKey(order.getExternalOrderId());
        TicketResponse ticket = ticketRequestService.requestTicket(ticketRequest);
        QrCodeResponse qrCode = level4Client.generateQrCode(new QrCodeRequest(ticket.getExternalTicketId()));
        recordPurchase(order, payment, request.getUserId(), ticket.getExternalTicketId(), "TICKET_PURCHASE", now);

        return TicketPurchaseResponse.builder()
                .ticketId(ticket.getExternalTicketId())
                .orderId(order.getExternalOrderId())
                .userId(request.getUserId())
                .ticketType(ticketType.getCode())
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
        CardType cardType = catalogService.requireActiveCardType(request.getPackageId());
        LocalDateTime now = LocalDateTime.now();

        Order order = createOrder(
                request.getUserId(),
                cardType.getPrice(),
                cardType.getCurrency(),
                "PROCESSING",
                now,
                List.of(createOrderItem(
                        "PHYSICAL_CARD",
                        cardType.getCode(),
                        cardType.getName(),
                        1,
                        cardType.getPrice())));
        Payment payment = createCompletedPayment(order, request.getUserId(), request.getPaymentMethod(), now);

        ExternalCardResponse externalCard = level5Client.requestCard(ExternalCardRequest.builder()
                .passengerAccountId(request.getUserId())
                .cardTypeCode(cardType.getCode())
                .orderId(order.getExternalOrderId())
                .requestSource("TICKET_WEB_APP")
                .build());
        requireCompleteCard(externalCard);

        PhysicalCard card = new PhysicalCard();
        card.setExternalCardId(externalCard.getExternalCardId());
        card.setPassengerAccountId(request.getUserId());
        card.setCardUid(externalCard.getCardUid());
        card.setMaskedCardNumber(externalCard.getMaskedCardNumber());
        card.setStatus(externalCard.getStatus());
        card.setIssuedAt(externalCard.getIssuedAt() == null ? now : externalCard.getIssuedAt());
        card.setExpiredAt(externalCard.getExpiresAt());
        card.setCachedAt(now);
        card.setExpiresAt(externalCard.getExpiresAt());
        PhysicalCard savedCard = physicalCardRepository.save(card);

        TicketRequest ticketRequest = new TicketRequest();
        ticketRequest.setPassengerAccountId(request.getUserId());
        ticketRequest.setTicketTypeCode(cardType.getCode());
        ticketRequest.setPhysicalCardExternalId(savedCard.getExternalCardId());
        ticketRequest.setIdempotencyKey(order.getExternalOrderId());
        ticketRequestService.requestTicket(ticketRequest);
        recordPurchase(order, payment, request.getUserId(), savedCard.getExternalCardId(), "CARD_PURCHASE", now);

        return CardPurchaseResponse.builder()
                .orderId(order.getExternalOrderId())
                .userId(request.getUserId())
                .cardId(savedCard.getExternalCardId())
                .cardUid(savedCard.getCardUid())
                .maskedCardNumber(savedCard.getMaskedCardNumber())
                .packageId(cardType.getCode())
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

    private void recordPurchase(
            Order order,
            Payment payment,
            String passengerAccountId,
            String purchasedItemId,
            String activityType,
            LocalDateTime occurredAt) {
        level5Client.recordPurchase(PurchaseActivityRequest.builder()
                .orderId(order.getExternalOrderId())
                .activityType(activityType)
                .passengerAccountId(passengerAccountId)
                .purchasedItemId(purchasedItemId)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .paymentId(payment.getExternalPaymentId())
                .occurredAt(occurredAt)
                .build());
    }

    private void requireCompleteCard(ExternalCardResponse card) {
        if (card == null || card.getExternalCardId() == null || card.getCardUid() == null
                || card.getMaskedCardNumber() == null || card.getStatus() == null) {
            throw new IllegalStateException("Level 5 returned an incomplete card");
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

}
