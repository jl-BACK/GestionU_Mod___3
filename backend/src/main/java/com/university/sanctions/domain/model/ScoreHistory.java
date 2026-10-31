package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "score_history", uniqueConstraints = @UniqueConstraint(
    name = "uq_score_origin", columnNames = {"student_code", "type", "origin_type", "origin_id"}))
@Getter @Setter @NoArgsConstructor
public class ScoreHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_code", nullable = false, length = 40)
    private String studentCode;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(name = "effective_points", nullable = false)
    private int effectivePoints;
    @Column(name = "score_before", nullable = false)
    private int scoreBefore;
    @Column(name = "score_after", nullable = false)
    private int scoreAfter;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(name = "origin_type", length = 80)
    private String originType;
    @Column(name = "origin_id", length = 120)
    private String originId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}