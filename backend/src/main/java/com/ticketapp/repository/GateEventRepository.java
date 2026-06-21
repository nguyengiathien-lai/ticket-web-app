package com.ticketapp.repository;

import com.ticketapp.entity.GateEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GateEventRepository extends JpaRepository<GateEvent, Long> {
}
