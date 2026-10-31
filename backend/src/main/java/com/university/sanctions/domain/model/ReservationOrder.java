package com.university.sanctions.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservation_order")
@Getter @Setter @NoArgsConstructor
public class ReservationOrder {
    @Id
    @Column(name = "reservation_id", length = 100)
    private String reservationId;
    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;
    @Column(nullable = false, length = 20)
    private String status;
}