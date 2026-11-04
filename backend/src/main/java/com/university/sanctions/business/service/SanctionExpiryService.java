package com.university.sanctions.business.service;

import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SanctionExpiryService {
    private final com.university.sanctions.domain.repository.SanctionRepository sanctions;
    private final Clock clock;
    public SanctionExpiryService(com.university.sanctions.domain.repository.SanctionRepository sanctions, Clock clock) {
        this.sanctions=sanctions; this.clock=clock;
    }
    @Scheduled(cron = "0 30 0 * * *", zone = "America/Bogota")
    @Transactional
    public void expire() {
        var yesterday = clock.instant().atZone(java.time.ZoneId.of("America/Bogota")).toLocalDate().minusDays(1);
        var due = sanctions.findByStatusAndEndsOnLessThanEqual("VIGENTE", yesterday);
        due.forEach(s -> s.setStatus("CUMPLIDA"));
        sanctions.saveAll(due);
    }
}