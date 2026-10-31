package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usage_record")
@Getter @Setter @NoArgsConstructor
public class UsageRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reservation_id", nullable = false, unique = true, length = 100)
    private String reservationId;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
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
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVA";
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
}