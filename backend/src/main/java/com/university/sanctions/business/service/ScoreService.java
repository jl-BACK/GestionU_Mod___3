package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.ScoreHistory;
import com.university.sanctions.domain.model.StudentRef;
import com.university.sanctions.domain.model.TrustScore;
import com.university.sanctions.domain.repository.ScoreHistoryRepository;
import com.university.sanctions.domain.repository.StudentRefRepository;
import com.university.sanctions.domain.repository.TrustScoreRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

    private final TrustScoreRepository scores;
    private final ScoreHistoryRepository history;
    private final StudentRefRepository students;
    private final ConfigurationService configuration;
    private final Clock clock;

    public ScoreService(
        TrustScoreRepository scores,
        ScoreHistoryRepository history,
        StudentRefRepository students,
        ConfigurationService configuration,
        Clock clock
    ) {
        this.scores = scores;
        this.history = history;
        this.students = students;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Transactional
    public TrustScore ensure(String code, String name) {
        if (code == null || code.isBlank()) {
            throw DomainException.badRequest("INVALID_STUDENT", "Falta el código institucional.");
        }

        students.findById(code).orElseGet(() ->
            students.save(new StudentRef(code, name == null || name.isBlank() ? code : name))
        );

        return scores.findById(code).orElseGet(() -> {
            TrustScore initial = new TrustScore(code, configuration.requiredInt("SCORE_INITIAL"));
            initial.setLastRecoveryAt(clock.instant());
            return scores.save(initial);
        });
    }

    public TrustScore getOrCreate(String code) {
        return ensure(code, code);
    }

    @Transactional
    public int apply(String code, int requestedDelta, String type, String reason, String originType, String originId) {
        ensure(code, code);

        if (originType != null && originId != null &&
            history.findFirstByStudentCodeAndTypeAndOriginTypeAndOriginId(code, type, originType, originId).isPresent()) {
            return scores.findById(code).orElseThrow().getScore();
        }

        TrustScore score = scores.findLocked(code).orElseThrow();
        int before = score.getScore();
        int max = configuration.requiredInt("SCORE_MAX");
        int after = Math.max(0, Math.min(max, before + requestedDelta));
        int effective = after - before;

        score.setScore(after);
        if (requestedDelta < 0) {
            score.setLastInfractionAt(clock.instant());
        } else if (requestedDelta > 0) {
            score.setLastRecoveryAt(clock.instant());
        }
        scores.save(score);

        ScoreHistory row = new ScoreHistory();
        row.setStudentCode(code);
        row.setType(type);
        row.setEffectivePoints(effective);
        row.setScoreBefore(before);
        row.setScoreAfter(after);
        row.setReason(reason);
        row.setOriginType(originType);
        row.setOriginId(originId);
        row.setCreatedAt(clock.instant());
        history.save(row);

        return after;
    }

    @Transactional
    public int restore(String code, int points, String reason, String originType, String originId) {
        return apply(code, Math.max(0, points), "RESTITUCION_ANULACION", reason, originType, originId);
    }

    @Transactional
    public void markInfraction(String code) {
        TrustScore score = scores.findLocked(code).orElseGet(() -> ensure(code, code));
        score.setLastInfractionAt(clock.instant());
        scores.save(score);
    }

    @Transactional
    public void refreshLastInfraction(String code, Instant instant) {
        TrustScore row = scores.findLocked(code).orElseThrow();
        row.setLastInfractionAt(instant);
        scores.save(row);
    }

    public TrustScore current(String code) {
        return scores.findById(code).orElseGet(() -> ensure(code, code));
    }

    public int effectivePoints(String code, String type, String originType, String originId) {
        return history.findFirstByStudentCodeAndTypeAndOriginTypeAndOriginId(code, type, originType, originId)
            .map(ScoreHistory::getEffectivePoints)
            .orElse(0);
    }
}
