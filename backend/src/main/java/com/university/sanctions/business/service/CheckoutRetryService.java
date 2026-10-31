package com.university.sanctions.business.service;

import com.university.sanctions.business.integration.ReservationModulePublisher;
import com.university.sanctions.domain.repository.CheckOutRecordRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import java.time.Clock;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutRetryService {
    private final CheckOutRecordRepository checkouts;
    private final ReservationCopyRepository reservations;
    private final ReservationModulePublisher publisher;
    private final Clock clock;
    public CheckoutRetryService(CheckOutRecordRepository checkouts, ReservationCopyRepository reservations,
        ReservationModulePublisher publisher, Clock clock) {
        this.checkouts=checkouts; this.reservations=reservations; this.publisher=publisher; this.clock=clock;
    }
    @Scheduled(fixedDelayString = "15000")
    @Transactional
    public void retrySpaceCheckouts() {
        for (var row : checkouts.findByAckStatusAndAcceptedFromBefore("REINTENTO", clock.instant())) {
            var reservation = reservations.findById(row.getReservationId()).orElse(null);
            if (reservation == null) continue;
            row.setAckStatus("PENDIENTE"); row.setAcceptedFrom(null); checkouts.save(row);
            publisher.checkOutRegistered(row.getReservationId(), Map.of(
                "reservationId", row.getReservationId(), "occurredAt", row.getCheckedOutAt().toString(),
                "verdict", row.isHasDamage() ? "REQUIERE_MANTENIMIENTO" : "SIN_NOVEDAD"), true);
        }
    }
}