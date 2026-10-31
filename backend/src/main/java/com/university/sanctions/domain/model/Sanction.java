package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sanction")
@Getter @Setter @NoArgsConstructor
public class Sanction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(nullable = false, length = 40)
    private String type = "INASISTENCIA";
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;
    @Column(name = "absence_confirmation_id", unique = true)
    private Long absenceConfirmationId;
    @Column(name = "lifted_reason", length = 1000)
    private String liftedReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}