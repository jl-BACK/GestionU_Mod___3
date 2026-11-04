package com.university.sanctions.business.service;

import com.university.sanctions.domain.repository.TrustScoreRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoveryService {
    private final TrustScoreRepository scores;
    private final ScoreService scoreService;
    private final ConfigurationService configuration;
    private final Clock clock;

    public RecoveryService(TrustScoreRepository scores, ScoreService scoreService, ConfigurationService configuration, Clock clock) {
        this.scores=scores; this.scoreService=scoreService; this.configuration=configuration; this.clock=clock;
    }

    @Scheduled(cron = "0 20 0 * * *", zone = "America/Bogota")
    @Transactional
    public void recoverAll() {
        Instant now = clock.instant();
        int days = configuration.requiredInt("RECOVERY_DAYS");
        int points = configuration.requiredInt("RECOVERY_POINTS");
        int max = configuration.requiredInt("SCORE_MAX");
        for (var score : scores.findByScoreLessThanOrderByLastInfractionAtAsc(max)) {
            Instant baseline = score.getLastInfractionAt();
            if (score.getLastRecoveryAt() != null && (baseline == null || score.getLastRecoveryAt().isAfter(baseline)))
                baseline = score.getLastRecoveryAt();
            if (baseline == null) continue;
            long intervals = Duration.between(baseline, now).toDays() / days;
            if (intervals < 1) continue;
            scoreService.apply(score.getStudentCode(), (int)Math.min(Integer.MAX_VALUE, intervals * points),
                "RECUPERACION", "Recuperación automática por " + (intervals * days) + " días sin infracciones.",
                "RECOVERY", now.atZone(ZoneId.of("America/Bogota")).toLocalDate().toString());
        }
    }
}