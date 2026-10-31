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
@Table(name = "lost_loan")
@Getter @Setter @NoArgsConstructor
public class LostLoan {
    @Id
    @Column(name = "reservation_id", length = 100)
    private String reservationId;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;
    @Column(name = "resource_name", length = 180)
    private String resourceName;
    @Column(name = "resource_type", length = 120)
    private String resourceType;
    @Column(name = "declared_at", nullable = false)
    private Instant declaredAt;
    @Column(name = "days_late", nullable = false)
    private int daysLate = 7;
    @Column(name = "charge_result", nullable = false, length = 30)
    private String chargeResult = "PENDIENTE";
}