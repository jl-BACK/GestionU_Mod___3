package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "absence_confirmation")
@Getter @Setter @NoArgsConstructor
public class AbsenceConfirmation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reservation_id", nullable = false, unique = true, length = 100)
    private String reservationId;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(name = "space_id", nullable = false, length = 100)
    private String spaceId;
    @Column(name = "space_name", length = 180)
    private String spaceName;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;
    @Column(name = "confirmed_by", nullable = false, length = 80)
    private String confirmedBy;
    @Column(length = 1000)
    private String note;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
    @Column(name = "accepted_from")
    private Instant acceptedFrom;
    @Column(name = "points_deducted", nullable = false)
    private int pointsDeducted;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}