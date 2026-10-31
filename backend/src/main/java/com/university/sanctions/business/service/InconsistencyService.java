package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.Inconsistency;
import com.university.sanctions.domain.repository.InconsistencyRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InconsistencyService {
    private final InconsistencyRepository repository;
    private final Clock clock;
    public InconsistencyService(InconsistencyRepository repository, Clock clock) { this.repository=repository; this.clock=clock; }

    @Transactional
    public void record(String type, String id, String message) {
        Inconsistency item = new Inconsistency(); item.setAggregateType(type); item.setAggregateId(id == null ? "unknown" : id);
        item.setDescription(message == null ? "Inconsistencia de integración." : message.substring(0, Math.min(1000, message.length())));
        item.setStatus("PENDIENTE"); item.setAttempts(0); item.setCreatedAt(clock.instant()); repository.save(item);
    }
}