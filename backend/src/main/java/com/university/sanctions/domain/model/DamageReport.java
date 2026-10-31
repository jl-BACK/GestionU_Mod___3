package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "damage_report")
@Getter @Setter @NoArgsConstructor
public class DamageReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usage_id", unique = true)
    private Long usageId;
    @Column(name = "reservation_id", length = 100)
    private String reservationId;
    @Column(name = "student_code", length = 40)
    private String studentCode;
    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;
    @Column(name = "resource_name", length = 180)
    private String resourceName;
    @Column(name = "resource_category", length = 20)
    private String resourceCategory;
    @Column(name = "resource_type", length = 120)
    private String resourceType;
    @Column(name = "resource_location", length = 180)
    private String resourceLocation;
    @Column(length = 180)
    private String faculty;
    @Column(name = "reported_by", nullable = false, length = 80)
    private String reportedBy;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(name = "evidence_path", length = 500)
    private String evidencePath;
    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;
    @Column(name = "issue_status", nullable = false, length = 20)
    private String issueStatus = "PENDIENTE";
    @Column(name = "charge_result", nullable = false, length = 30)
    private String chargeResult = "PENDIENTE";
}