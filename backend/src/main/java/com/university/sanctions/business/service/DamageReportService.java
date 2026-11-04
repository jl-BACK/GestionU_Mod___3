package com.university.sanctions.business.service;

import com.university.sanctions.business.integration.UsageEventsPublisher;
import com.university.sanctions.domain.model.DamageReport;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import com.university.sanctions.domain.repository.UsageRecordRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DamageReportService {
    private final DamageReportRepository reports;
    private final ReservationCopyRepository reservations;
    private final UsageRecordRepository usages;
    private final UsageEventsPublisher publisher;
    private final ScoreService scores;
    private final Clock clock;

    public DamageReportService(DamageReportRepository reports, ReservationCopyRepository reservations,
        UsageRecordRepository usages, UsageEventsPublisher publisher, ScoreService scores, Clock clock) {
        this.reports=reports; this.reservations=reservations; this.usages=usages; this.publisher=publisher; this.scores=scores; this.clock=clock;
    }

    @Transactional
    public DamageReport report(String resourceId, String reservationId, String studentCode, String description, String evidencePath) {
        if (description == null || description.isBlank()) throw DomainException.badRequest("DAMAGE_DESCRIPTION_REQUIRED", "La descripción del daño es obligatoria.");
        var usage = reservationId == null ? usages.findFirstByResourceIdOrderByStartedAtDesc(resourceId).orElse(null)
            : usages.findByReservationId(reservationId).orElse(null);
        var reservation = reservationId != null ? reservations.findById(reservationId).orElse(null)
            : usage == null ? null : reservations.findById(usage.getReservationId()).orElse(null);
        DamageReport report = new DamageReport();
        report.setResourceId(resourceId); report.setReservationId(reservation == null ? reservationId : reservation.getReservationId());
        report.setStudentCode(reservation == null ? studentCode : reservation.getHolderCode());
        if (reservation != null) {
            report.setResourceName(reservation.getResourceName()); report.setResourceCategory(reservation.getResourceCategory());
            report.setResourceType(reservation.getResourceType()); report.setResourceLocation(reservation.getResourceLocation());
            report.setFaculty(reservation.getFaculty());
            if (usage != null) report.setUsageId(usage.getId());
            else usages.findByReservationId(reservation.getReservationId()).ifPresent(found -> report.setUsageId(found.getId()));
        }
        if (report.getUsageId() != null && reports.existsByUsageId(report.getUsageId()))
            throw DomainException.conflict("DAMAGE_ALREADY_REPORTED", "Ya existe una novedad técnica para esta utilización.");
        report.setReportedBy(studentCode); report.setDescription(description.trim()); report.setEvidencePath(evidencePath);
        report.setReportedAt(clock.instant()); report.setIssueStatus("PENDIENTE"); report.setChargeResult("PENDIENTE");
        reports.save(report);
        if (report.getStudentCode() != null) scores.markInfraction(report.getStudentCode());
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("resourceId", resourceId); data.put("reservationId", report.getReservationId());
        data.put("studentCode", report.getStudentCode()); data.put("description", report.getDescription());
        data.put("reportedAt", report.getReportedAt().toString());
        publisher.damageReported(resourceId, data);
        return report;
    }
}