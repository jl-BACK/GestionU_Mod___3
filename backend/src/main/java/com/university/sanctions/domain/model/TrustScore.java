package com.university.sanctions.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trust_score")
@Getter @Setter @NoArgsConstructor
public class TrustScore {
    @Id
    @Column(name = "student_code", length = 40)
    private String studentCode;
    @Column(nullable = false)
    private int score;
    @Column(name = "last_infraction_at")
    private Instant lastInfractionAt;
    @Column(name = "last_recovery_at")
    private Instant lastRecoveryAt;
    @Version
    private long version;

    public TrustScore(String studentCode, int score) {
        this.studentCode = studentCode;
        this.score = score;
    }
}