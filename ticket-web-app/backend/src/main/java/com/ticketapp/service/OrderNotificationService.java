package com.ticketapp.service;

import com.ticketapp.dto.order.OrderNotificationResponse;
import com.ticketapp.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderNotificationService {

    private final OrderRepository orderRepository;

    public OrderNotificationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderNotificationResponse> getForPassenger(String accountId) {
        return orderRepository.findByPassengerAccountIdOrderByOrderedAtDesc(accountId)
                .stream()
                .map(OrderNotificationResponse::from)
                .toList();
    }
}
