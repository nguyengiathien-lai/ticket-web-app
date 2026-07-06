package com.ticketapp.service;

import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalPassTicketRequest;
import com.ticketapp.dto.external.ExternalSingleTripTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import com.ticketapp.dto.purchase.TicketPurchaseRequest;
import com.ticketapp.dto.purchase.TicketPurchaseResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Account;
import com.ticketapp.entity.FarePackage;
import com.ticketapp.entity.Order;
import com.ticketapp.entity.OrderItem;
import com.ticketapp.entity.Payment;
import com.ticketapp.entity.PhysicalCard;
import com.ticketapp.repository.OrderRepository;
import com.ticketapp.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class PurchaseService {

    private final AccountService accountService;
    private final TicketService ticketService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CardService cardService;
    private final Level4Client level4Client;
    private final Level5Client level5Client;
    private final EmailService emailService;

    public PurchaseService(
            AccountService accountService,
            TicketService ticketService,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CardService cardService,
            Level4Client level4Client,
            Level5Client level5Client,
            EmailService emailService) {
        this.accountService = accountService;
        this.ticketService = ticketService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.cardService = cardService;
        this.level4Client = level4Client;
        this.level5Client = level5Client;
        this.emailService = emailService;
    }

    @Transactional
    public TicketPurchaseResponse purchaseTicket(TicketPurchaseRequest request) {
        Account account = requirePurchasableAccount(request.getUserId());
        LocalDateTime now = LocalDateTime.now();
        FarePackage farePackage = findFarePackage(request.getPackageId());

        TicketRequest ticketRequest = new TicketRequest();
        ticketRequest.setPassengerAccountId(request.getUserId());
        ticketRequest.setTicketTypeCode(firstText(request.getPackageId(), request.getTicketType()));

        ExternalTicketResponse externalTicket = purchaseExternalTicket(request);
        TicketResponse ticket = ticketService.cacheExternalTicket(ticketRequest, externalTicket);
        // QrCodeResponse qrCode = level4Client.generateQrCode(new QrCodeRequest(ticket.getExternalTicketId()));
        BigDecimal totalAmount = firstValue(ticket.getFare(), farePackage == null ? null : farePackage.getPrice(), BigDecimal.ZERO);
        String currency = firstText(ticket.getCurrency(), farePackage == null ? null : farePackage.getCurrency(), "VND");
        String itemCode = firstText(ticket.getTicketTypeCode(), request.getPackageId(), request.getTicketType(), "TICKET");
        String itemName = farePackage == null ? itemCode : farePackage.getName();

        Order order = createOrder(
                request.getUserId(),
                totalAmount,
                currency,
                "COMPLETED",
                now,
                List.of(createOrderItem("TICKET", itemCode, itemName, 1, totalAmount)));
        Payment payment = createCompletedPayment(order, request.getUserId(), firstText(request.getPaymentMethod(), "VNPAY"), now);

        TicketPurchaseResponse response = TicketPurchaseResponse.builder()
                .ticketId(ticket.getExternalTicketId())
                .orderId(order.getExternalOrderId())
                .userId(request.getUserId())
                .packageId(itemCode)
                .origin(ticket.getFromStationCode())
                .destination(ticket.getToStationCode())
                .totalPrice(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(firstText(ticket.getStatus(), "confirmed"))
                .qrCode(null)
                .confirmationNumber(order.getOrderCode())
                .paymentId(payment.getExternalPaymentId())
                .paymentStatus(payment.getStatus())
                .purchasedAt(now)
                .build();
        runAfterCommit(() -> sendTicketPurchaseConfirmation(account, response));

        return response;
    }

    @Transactional
    public TicketPurchaseResponse purchaseSingleTripTicket(TicketPurchaseRequest request) {
        log.info("Purchasing single-trip ticket for user: {}, from: {}, to: {}",
                request.getUserId(), request.getFromStationId(), request.getToStationId());
        request.setTicketType("SINGLE_TRIP");
        return purchaseTicket(request);
    }

    @Transactional
    public TicketPurchaseResponse purchaseMonthlyPassTicket(TicketPurchaseRequest request) {
        log.info("Purchasing monthly pass ticket for user: {}, route: {}",
                request.getUserId(), request.getRouteId());
        request.setTicketType("MONTHLY_PASS");
        return purchaseTicket(request);
    }

    private ExternalTicketResponse purchaseExternalTicket(TicketPurchaseRequest request) {
        if (isSingleTrip(request)) {
            log.info("Purchasing single-trip ticket for user: {}, from: {}, to: {}",
                    request.getUserId(), request.getFromStationId(), request.getToStationId());
            requireText(request.getFromStationId(), "fromStationId is required for single-trip tickets");
            requireText(request.getToStationId(), "toStationId is required for single-trip tickets");
            return level5Client.purchaseSingleTripTicket(ExternalSingleTripTicketRequest.builder()
                    .userId(request.getUserId())
                    .fromStationId(request.getFromStationId())
                    .toStationId(request.getToStationId())
                    .mode(firstText(request.getMode(), inferMode(request.getPackageId()), "METRO"))
                    .build());
        }

        log.info("Purchasing monthly pass ticket for user: {}, route: {}",
                request.getUserId(), request.getRouteId());
        String mode = firstText(request.getMode(), inferMode(request.getPackageId()), "METRO");
        boolean metroPass = "METRO".equalsIgnoreCase(mode);
        return level5Client.purchasePassTicket(ExternalPassTicketRequest.builder()
                .userId(request.getUserId())
                .mode(mode)
                .scope(metroPass ? null : request.getScope())
                .routeId(metroPass ? null : requireText(request.getRouteId(), null))
                .passengerType(firstText(request.getPassengerType(), "ADULT"))
                .validFrom(firstValue(request.getValidFrom(), LocalDate.now()))
                .durationType(firstText(request.getDurationType()))
                .durationMonths(firstValue(request.getDurationMonths(), null))
                .build());
    }

    private boolean isSingleTrip(TicketPurchaseRequest request) {
        String value = firstText(request.getTicketType(), request.getPackageId(), request.getDurationType(), "");
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("single") && !normalized.contains("month");
    }

    private String inferMode(String packageId) {
        if (packageId == null) {
            return null;
        }

        String normalized = packageId.toUpperCase(Locale.ROOT);
        if (normalized.contains("BUS")) {
            return "BUS";
        }
        if (normalized.contains("METRO")) {
            return "METRO";
        }
        return null;
    }

    private FarePackage findFarePackage(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        try {
            return ticketService.requireActiveFarePackage(code);
        } catch (RuntimeException exception) {
            log.debug("No local fare package found for ticket purchase code: {}", code);
            return null;
        }
    }

    @Transactional
    public CardPurchaseResponse purchaseCard(CardPurchaseRequest request) {
        LocalDateTime now = LocalDateTime.now();
        CardPurchaseRequest.CardDetails cardDetails = request.getCard();
        CardPurchaseRequest.TicketDetails ticketDetails = request.getTicket();
        String userId = requireSamePassenger(cardDetails, ticketDetails);
        Account account = requirePurchasableAccount(userId);
        String paymentMethod = "VNPAY";

        CardPurchaseResponse externalPurchase = level5Client.purchaseCard(normalizeCardPurchaseRequest(request, userId));
        requireCompleteCard(externalPurchase);
        CardPurchaseResponse.IssuedCard issuedCard = externalPurchase.getCard();
        CardPurchaseResponse.IssuedTicket issuedTicket = externalPurchase.getTicket();

        PhysicalCard card = new PhysicalCard();
        card.setExternalCardId(issuedCard.getId());
        card.setPassengerAccountId(userId);
        card.setCardUid(firstText(cardDetails == null ? null : cardDetails.getCardUid(),
                issuedCard.getCardUid(), issuedCard.getId()));
        card.setStatus(coalesce(issuedCard.getStatus(), "ACTIVE"));
        card.setIssuedAt(firstValue(issuedCard.getActivatedAt(), issuedCard.getCreatedAt(), now));
        card.setCachedAt(now);
        PhysicalCard savedCard = cardService.cacheCard(card);

        TicketRequest ticketRequest = new TicketRequest();
        ticketRequest.setPassengerAccountId(userId);
        ticketRequest.setTicketTypeCode("MONTHLY_PASS");
        ticketRequest.setPhysicalCardExternalId(savedCard.getExternalCardId());
        ExternalTicketResponse externalTicket = toExternalTicketResponse(issuedTicket, savedCard.getExternalCardId(), userId);
        TicketResponse ticket = ticketService.cacheExternalTicket(ticketRequest, externalTicket);

        BigDecimal totalAmount = firstValue(ticket.getFare(), BigDecimal.ZERO);
        String currency = firstText(ticket.getCurrency(), "VND");
        Order order = createOrder(
                userId,
                totalAmount,
                currency,
                "COMPLETED",
                now,
                List.of(createOrderItem("MONTHLY_PASS", "MONTHLY_PASS", "Monthly pass", 1, totalAmount)));
        createCompletedPayment(order, userId, paymentMethod, now);

        CardPurchaseResponse response = CardPurchaseResponse.builder()
                .card(CardPurchaseResponse.IssuedCard.builder()
                        .id(savedCard.getExternalCardId())
                        .cardUid(savedCard.getCardUid())
                        .status(savedCard.getStatus())
                        .type(firstText(issuedCard.getType(), "MONTHLY_PASS"))
                        .supportsMetro(firstValue(cardDetails == null ? null : cardDetails.getSupportsMetro(),
                                issuedCard.getSupportsMetro(), supportsMode(ticket.getMode(), "METRO")))
                        .supportsBus(firstValue(cardDetails == null ? null : cardDetails.getSupportsBus(),
                                issuedCard.getSupportsBus(), supportsMode(ticket.getMode(), "BUS")))
                        .issuedAtStationId(issuedCard.getIssuedAtStationId())
                        .linkedUserId(userId)
                        .activatedAt(savedCard.getIssuedAt())
                        .createdAt(firstValue(issuedCard.getCreatedAt(), now))
                        .updatedAt(firstValue(issuedCard.getUpdatedAt(), now))
                        .build())
                .ticket(CardPurchaseResponse.IssuedTicket.builder()
                        .ticketId(ticket.getExternalTicketId())
                        .type(firstText(issuedTicket.getType(), ticket.getTicketTypeCode(), "MONTHLY_PASS"))
                        .mode(ticket.getMode())
                        .scope(ticket.getScope())
                        .routeId(issuedTicket.getRouteId())
                        .status(ticket.getStatus())
                        .cardId(savedCard.getExternalCardId())
                        .userId(userId)
                        .fromStationCode(ticket.getFromStationCode())
                        .toStationCode(ticket.getToStationCode())
                        .price(ticket.getFare())
                        .fareRuleId(issuedTicket.getFareRuleId())
                        .discountId(issuedTicket.getDiscountId())
                        .validFrom(toLocalDate(ticket.getValidFrom()))
                        .validTo(toLocalDate(ticket.getValidUntil()))
                        .purchasedAt(firstValue(ticket.getIssuedAt(), now))
                        .build())
                .build();
        runAfterCommit(() -> sendCardPurchaseConfirmation(
                account,
                order.getExternalOrderId(),
                savedCard.getExternalCardId(),
                "MONTHLY_PASS",
                null,
                order.getTotalAmount(),
                order.getCurrency()));

        return response;
    }

    private String requireSamePassenger(
            CardPurchaseRequest.CardDetails cardDetails,
            CardPurchaseRequest.TicketDetails ticketDetails) {
        String userId = requireText(firstText(
                cardDetails == null ? null : cardDetails.getUserId(),
                ticketDetails == null ? null : ticketDetails.getUserId()),
                "userId is required");
        requireMatchingUser(userId, cardDetails == null ? null : cardDetails.getUserId(), "card.userId");
        requireMatchingUser(userId, ticketDetails == null ? null : ticketDetails.getUserId(), "ticket.userId");
        return userId;
    }

    private CardPurchaseRequest normalizeCardPurchaseRequest(CardPurchaseRequest request, String userId) {
        CardPurchaseRequest normalized = new CardPurchaseRequest();
        CardPurchaseRequest.CardDetails card = request.getCard() == null
                ? new CardPurchaseRequest.CardDetails()
                : request.getCard();
        CardPurchaseRequest.TicketDetails ticket = request.getTicket() == null
                ? new CardPurchaseRequest.TicketDetails()
                : request.getTicket();

        card.setUserId(firstText(card.getUserId(), userId));
        ticket.setUserId(firstText(ticket.getUserId(), userId));
        ticket.setMode(firstText(ticket.getMode(), "METRO"));
        ticket.setScope(firstText(ticket.getScope(), "SINGLE_ROUTE"));
        ticket.setRouteId(requireText(ticket.getRouteId(), "routeId is required for monthly pass tickets"));
        ticket.setPassengerType(firstText(ticket.getPassengerType(), "ADULT"));
        ticket.setValidFrom(firstValue(ticket.getValidFrom(), LocalDate.now()));
        ticket.setDurationType(firstText(ticket.getDurationType(), "MONTHLY"));
        ticket.setDurationMonths(firstValue(ticket.getDurationMonths(), 1));

        normalized.setCard(card);
        normalized.setTicket(ticket);
        return normalized;
    }

    private ExternalTicketResponse toExternalTicketResponse(
            CardPurchaseResponse.IssuedTicket ticket,
            String cardId,
            String userId) {
        ExternalTicketResponse externalTicket = new ExternalTicketResponse();
        externalTicket.setExternalTicketId(ticket.getTicketId());
        externalTicket.setPassengerAccountId(firstText(ticket.getUserId(), userId));
        externalTicket.setTicketTypeCode(firstText(ticket.getType(), "MONTHLY_PASS"));
        externalTicket.setPhysicalCardExternalId(firstText(ticket.getCardId(), cardId));
        externalTicket.setStatus(ticket.getStatus());
        externalTicket.setMode(ticket.getMode());
        externalTicket.setScope(ticket.getScope());
        externalTicket.setRouteId(ticket.getRouteId());
        externalTicket.setFareRuleId(ticket.getFareRuleId());
        externalTicket.setDiscountId(ticket.getDiscountId());
        externalTicket.setFare(ticket.getPrice());
        externalTicket.setCurrency("VND");
        externalTicket.setFromStationCode(ticket.getFromStationCode());
        externalTicket.setToStationCode(ticket.getToStationCode());
        externalTicket.setValidFrom(ticket.getValidFrom() == null ? null : ticket.getValidFrom().atStartOfDay());
        externalTicket.setValidUntil(ticket.getValidTo() == null ? null : ticket.getValidTo().atTime(23, 59, 59));
        externalTicket.setIssuedAt(ticket.getPurchasedAt());
        externalTicket.setExpiresAt(externalTicket.getValidUntil());
        externalTicket.setExpired(false);
        return externalTicket;
    }

    private void requireMatchingUser(String expectedUserId, String actualUserId, String fieldName) {
        if (actualUserId != null && !actualUserId.isBlank() && !expectedUserId.equals(actualUserId.trim())) {
            throw new IllegalArgumentException(fieldName + " must match userId");
        }
    }

    private Boolean supportsMode(String mode, String expectedMode) {
        return mode != null && mode.equalsIgnoreCase(expectedMode);
    }

    private LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private Account requirePurchasableAccount(String userId) {
        return accountService.findById(userId)
                .filter(account -> account.getIsActive() && account.getIsEmailVerified())
                .orElseThrow(() -> new IllegalArgumentException("Passenger account not found, inactive, or unverified"));
    }

    private void sendTicketPurchaseConfirmation(Account account, TicketPurchaseResponse response) {
        try {
            emailService.sendTicketPurchaseConfirmed(
                    account.getEmail(),
                    account.getFullName(),
                    response.getConfirmationNumber(),
                    response.getTicketId(),
                    response.getPackageId(),
                    response.getTotalPrice(),
                    response.getCurrency(),
                    response.getPurchasedAt());
        } catch (RuntimeException exception) {
            log.warn("Ticket purchase confirmation email failed for order: {}", response.getOrderId(), exception);
        }
    }

    private void sendCardPurchaseConfirmation(
            Account account,
            String orderId,
            String cardId,
            String packageId,
            String deliveryAddress,
            BigDecimal totalPrice,
            String currency) {
        try {
            emailService.sendCardPurchaseConfirmed(
                    account.getEmail(),
                    account.getFullName(),
                    orderId,
                    cardId,
                    packageId,
                    deliveryAddress,
                    deliveryAddress == null || deliveryAddress.isBlank() ? null : LocalDate.now().plusDays(3),
                    totalPrice,
                    currency);
        } catch (RuntimeException exception) {
            log.warn("Card purchase confirmation email failed for order: {}", orderId, exception);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
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

    private void requireCompleteCard(CardPurchaseResponse purchase) {
        if (purchase == null
                || purchase.getCard() == null
                || purchase.getCard().getId() == null
                || purchase.getCard().getId().isBlank()) {
            throw new IllegalStateException("Level 5 returned an incomplete card");
        }
        if (purchase.getTicket() == null
                || purchase.getTicket().getTicketId() == null
                || purchase.getTicket().getTicketId().isBlank()) {
            throw new IllegalStateException("Level 5 returned an incomplete card ticket");
        }
    }

    private String coalesce(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstValue(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String shortToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

}
