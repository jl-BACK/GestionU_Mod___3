package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unblock_audit")
@Getter @Setter @NoArgsConstructor
public class UnblockAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(name = "unblocked_by", nullable = false, length = 80)
    private String unblockedBy;
    @Column(name = "unblocked_at", nullable = false)
    private Instant unblockedAt;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(name = "score_before", nullable = false)
    private int scoreBefore;
    @Column(name = "score_after", nullable = false)
    private int scoreAfter;
    @Column(name = "sanctions_lifted", nullable = false)
    private int sanctionsLifted;
}