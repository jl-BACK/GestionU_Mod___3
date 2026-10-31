package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "check_out")
@Getter @Setter @NoArgsConstructor
public class CheckOutRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usage_id", nullable = false, unique = true)
    private Long usageId;
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
    @Column(name = "checked_out_at", nullable = false)
    private Instant checkedOutAt;
    @Column(nullable = false, length = 20)
    private String result;
    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes;
    @Column(name = "has_damage", nullable = false)
    private boolean hasDamage;
    @Column(name = "damage_description", length = 2000)
    private String damageDescription;
    @Column(name = "ack_status", nullable = false, length = 20)
    private String ackStatus = "PENDIENTE";
    @Column(name = "ack_reason", length = 500)
    private String ackReason;
    @Column(name = "accepted_from")
    private Instant acceptedFrom;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
}