package com.ticketapp.repository;

import com.ticketapp.entity.PhysicalCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalCardRepository extends JpaRepository<PhysicalCard, Long> {

    Optional<PhysicalCard> findByExternalCardId(String externalCardId);

    Optional<PhysicalCard> findByCardUid(String cardUid);

    List<PhysicalCard> findByPassengerAccountIdOrderByIssuedAtDesc(String passengerAccountId);
}
