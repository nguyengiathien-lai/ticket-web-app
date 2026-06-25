package com.validationgate.repository;

import com.validationgate.entity.TapEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TapEventRepository extends JpaRepository<TapEvent, Long> {
    List<TapEvent> findByDeliveryStatusInOrderByRecordedAtAsc(Collection<String> statuses, Pageable pageable);

    long deleteByDeliveryStatusAndSentAtBefore(String deliveryStatus, LocalDateTime sentAt);
}
