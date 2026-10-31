package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "absence_annulment_audit")
@Getter @Setter @NoArgsConstructor
public class AbsenceAnnulmentAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "confirmation_id", nullable = false)
    private Long confirmationId;
    @Column(name = "annulled_by", nullable = false, length = 80)
    private String annulledBy;
    @Column(name = "annulled_at", nullable = false)
    private Instant annulledAt;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(name = "points_restored", nullable = false)
    private int pointsRestored;
}