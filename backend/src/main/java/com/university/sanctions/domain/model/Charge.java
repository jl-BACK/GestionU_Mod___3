package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "charge")
@Getter @Setter @NoArgsConstructor
public class Charge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "damage_report_id", unique = true)
    private Long damageReportId;
    @Column(name = "lost_loan_reservation_id", unique = true, length = 100)
    private String lostLoanReservationId;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;
    @Column(name = "resource_name", length = 180)
    private String resourceName;
    @Column(nullable = false, length = 200)
    private String reason;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 20)
    private String status = "PENDIENTE";
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}