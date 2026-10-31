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
@Table(name = "reservation_copy")
@Getter @Setter @NoArgsConstructor
public class ReservationCopy {
    @Id
    @Column(name = "reservation_id", length = 100)
    private String reservationId;
    @Column(nullable = false, length = 40)
    private String origin = "ESTUDIANTE";
    @Column(nullable = false)
    private boolean sanctionable = true;
    @Column(name = "holder_code", nullable = false, length = 40)
    private String holderCode;
    @Column(name = "holder_name", length = 180)
    private String holderName;
    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;
    @Column(name = "resource_name", length = 180)
    private String resourceName;
    @Column(name = "resource_category", nullable = false, length = 20)
    private String resourceCategory;
    @Column(name = "resource_type", length = 120)
    private String resourceType;
    @Column(name = "resource_location", length = 180)
    private String resourceLocation;
    @Column(length = 180)
    private String faculty;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;
    @Column(name = "loan_withdrawn_at")
    private Instant loanWithdrawnAt;
    @Column(name = "loan_due_at")
    private Instant loanDueAt;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVA";
    @Column(name = "score_at_due")
    private Integer scoreAtDue;
    @Column(name = "last_event_at")
    private Instant lastEventAt;
}