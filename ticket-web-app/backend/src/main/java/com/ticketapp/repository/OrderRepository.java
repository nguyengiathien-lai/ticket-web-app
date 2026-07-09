package com.ticketapp.repository;

import com.ticketapp.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByExternalOrderId(String externalOrderId);

    List<Order> findByPassengerAccountIdOrderByOrderedAtDesc(String passengerAccountId);
}
