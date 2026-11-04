package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.Sanction;
import com.university.sanctions.domain.model.UnblockAudit;
import com.university.sanctions.domain.repository.SanctionRepository;
import com.university.sanctions.domain.repository.TrustScoreRepository;
import com.university.sanctions.domain.repository.UnblockAuditRepository;
import com.university.sanctions.presentation.api.ApiModels.UnblockResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnblockService {

    private final TrustScoreRepository scores;
    private final SanctionRepository sanctions;
    private final UnblockAuditRepository audits;
    private final ScoreService scoreService;
    private final ConfigurationService configuration;
    private final Clock clock;

    public UnblockService(
        TrustScoreRepository scores,
        SanctionRepository sanctions,
        UnblockAuditRepository audits,
        ScoreService scoreService,
        ConfigurationService configuration,
        Clock clock
    ) {
        this.scores = scores;
        this.sanctions = sanctions;
        this.audits = audits;
        this.scoreService = scoreService;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Transactional
    public UnblockResponse unblock(String code, String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw DomainException.badRequest("REASON_REQUIRED", "El motivo del desbloqueo es obligatorio.");
        }

        scoreService.ensure(code, code);
        var locked = scores.findLocked(code).orElseThrow();
        int before = locked.getScore();
        if (before != 0) {
            throw DomainException.conflict("STUDENT_NOT_BLOCKED", "Solo se puede desbloquear a estudiantes con score 0.");
        }

        int target = configuration.requiredInt("UNBLOCK_SCORE");

        UnblockAudit audit = new UnblockAudit();
        audit.setStudentCode(code);
        audit.setUnblockedBy(actor);
        audit.setUnblockedAt(clock.instant());
        audit.setReason(reason);
        audit.setScoreBefore(before);
        audit.setScoreAfter(target);

        scoreService.apply(code, target - before, "DESBLOQUEO_MANUAL", reason, "UNBLOCK", UUID.randomUUID().toString());

        List<Sanction> active = sanctions.findByStudentCodeAndStatusInOrderByStartsOnDesc(code, List.of("VIGENTE"));
        int lifted = 0;
        LocalDate today = clock.instant().atZone(ZoneId.of("America/Bogota")).toLocalDate();
        for (Sanction sanction : active) {
            if (!sanction.getEndsOn().isBefore(today)) {
                sanction.setStatus("LEVANTADA");
                sanction.setLiftedReason(reason);
                lifted++;
            }
        }
        sanctions.saveAll(active);

        audit.setSanctionsLifted(lifted);
        audits.save(audit);

        return new UnblockResponse(code, before, target, lifted);
    }
}
