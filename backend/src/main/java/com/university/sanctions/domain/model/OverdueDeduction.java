package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "overdue_deduction", uniqueConstraints = @UniqueConstraint(
    name = "uq_overdue_student_day", columnNames = {"student_code", "deduction_date"}))
@Getter @Setter @NoArgsConstructor
public class OverdueDeduction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reservation_id", nullable = false, length = 100)
    private String reservationId;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(name = "deduction_date", nullable = false)
    private LocalDate deductionDate;
    @Column(nullable = false)
    private int points;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}