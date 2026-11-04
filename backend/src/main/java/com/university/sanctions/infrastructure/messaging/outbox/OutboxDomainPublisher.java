package com.university.sanctions.infrastructure.messaging.outbox;

import com.university.sanctions.business.integration.ReservationModulePublisher;
import com.university.sanctions.business.integration.UsageEventsPublisher;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OutboxDomainPublisher implements UsageEventsPublisher, ReservationModulePublisher {
    private final OutboxService outbox;

    public OutboxDomainPublisher(OutboxService outbox) {
        this.outbox = outbox;
    }

    @Override public void reservationFinalized(String resourceId, Map<String, Object> data) {
        outbox.enqueueM1("reserva.finalizada", resourceId, data);
    }

    @Override public void damageReported(String resourceId, Map<String, Object> data) {
        outbox.enqueueM1("reporte.danos", resourceId, data);
    }

    @Override public void checkOutRegistered(String reservationId, Map<String, Object> data, boolean space) {
        outbox.enqueueM2("module3.reservation.check-out.v1", "CheckOutRegistered", reservationId, data);
    }

    @Override public void noShowReported(String reservationId, Map<String, Object> data) {
        outbox.enqueueM2("module3.reservation.no-show.v1", "NoShowReported", reservationId, data);
    }

    @Override public void noShowReportVoided(String reservationId, Map<String, Object> data) {
        outbox.enqueueM2("module3.reservation.no-show.v1", "NoShowReportVoided", reservationId, data);
    }
}