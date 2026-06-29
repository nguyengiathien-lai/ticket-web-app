package com.validationgate.repository;

import com.validationgate.dto.TapEventType;
import com.validationgate.entity.TapEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TapEventRepositoryTest {

    @Autowired
    private TapEventRepository repository;

    @Test
    void findsRetryableEventsIgnoringStatusCaseAndWhitespaceInRecordedOrder() {
        TapEvent pendingWithWhitespace = event(
                "AFCQR:v1:ticket-1:exp=9999999999:hmac=test",
                " PENDING ",
                LocalDateTime.of(2026, 6, 21, 15, 30));
        TapEvent failedLowercase = event(
                "AFCQR:v1:ticket-2:exp=9999999999:hmac=test",
                "failed",
                LocalDateTime.of(2026, 6, 21, 15, 29));
        TapEvent sent = event(
                "AFCQR:v1:ticket-3:exp=9999999999:hmac=test",
                "SENT",
                LocalDateTime.of(2026, 6, 21, 15, 28));
        repository.saveAll(List.of(pendingWithWhitespace, failedLowercase, sent));

        List<TapEvent> batch = repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                List.of("PENDING", "FAILED"),
                PageRequest.of(0, 10));

        assertThat(batch)
                .extracting(TapEvent::getQrPayload)
                .containsExactly(failedLowercase.getQrPayload(), pendingWithWhitespace.getQrPayload());
    }

    @Test
    void appliesTheRequestedBatchSize() {
        repository.saveAll(List.of(
                event("AFCQR:v1:ticket-1:exp=9999999999:hmac=test", "PENDING",
                        LocalDateTime.of(2026, 6, 21, 15, 28)),
                event("AFCQR:v1:ticket-2:exp=9999999999:hmac=test", "PENDING",
                        LocalDateTime.of(2026, 6, 21, 15, 29))));

        List<TapEvent> batch = repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                List.of("PENDING", "FAILED"),
                PageRequest.of(0, 1));

        assertThat(batch)
                .extracting(TapEvent::getQrPayload)
                .containsExactly("AFCQR:v1:ticket-1:exp=9999999999:hmac=test");
    }

    private TapEvent event(String qrPayload, String deliveryStatus, LocalDateTime recordedAt) {
        TapEvent event = new TapEvent();
        event.setQrPayload(qrPayload);
        event.setEventType(TapEventType.TAP_IN);
        event.setRecordedAt(recordedAt);
        event.setDeliveryStatus(deliveryStatus);
        return event;
    }
}
