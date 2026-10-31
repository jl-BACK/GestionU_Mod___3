package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "charge_audit")
@Getter @Setter @NoArgsConstructor
public class ChargeAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "charge_id", nullable = false)
    private Long chargeId;
    @Column(name = "changed_by", nullable = false, length = 80)
    private String changedBy;
    @Column(name = "previous_status", nullable = false, length = 20)
    private String previousStatus;
    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;
    @Column(length = 1000)
    private String reason;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}