package com.university.sanctions.business.service;

import com.university.sanctions.business.integration.ReservationModulePublisher;
import com.university.sanctions.business.integration.UsageEventsPublisher;
import com.university.sanctions.domain.model.CheckOutRecord;
import com.university.sanctions.domain.model.DamageReport;
import com.university.sanctions.domain.model.ReservationCopy;
import com.university.sanctions.domain.model.UsageRecord;
import com.university.sanctions.domain.repository.CheckOutRecordRepository;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import com.university.sanctions.domain.repository.UsageRecordRepository;
import com.university.sanctions.presentation.api.ApiModels.CheckOutResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckOutService {

    private final ReservationCopyRepository reservations;
    private final UsageRecordRepository usages;
    private final CheckOutRecordRepository checkouts;
    private final DamageReportRepository damageReports;
    private final UsageEventsPublisher usagePublisher;
    private final ReservationModulePublisher reservationPublisher;
    private final ScoreService scores;
    private final ConfigurationService configuration;
    private final Clock clock;

    public CheckOutService(
        ReservationCopyRepository reservations,
        UsageRecordRepository usages,
        CheckOutRecordRepository checkouts,
        DamageReportRepository damageReports,
        UsageEventsPublisher usagePublisher,
        ReservationModulePublisher reservationPublisher,
        ScoreService scores,
        ConfigurationService configuration,
        Clock clock
    ) {
        this.reservations = reservations;
        this.usages = usages;
        this.checkouts = checkouts;
        this.damageReports = damageReports;
        this.usagePublisher = usagePublisher;
        this.reservationPublisher = reservationPublisher;
        this.scores = scores;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Transactional
    public CheckOutResponse checkOut(String student, String reservationId, boolean hasDamage, String description) {
        if (hasDamage && (description == null || description.isBlank())) {
            throw DomainException.badRequest("DAMAGE_DESCRIPTION_REQUIRED", "Describe el daño antes de registrar la devolución.");
        }

        ReservationCopy reservation = reservations.findById(reservationId)
            .orElseThrow(() -> DomainException.notFound("RESERVATION_NOT_FOUND", "M2 aún no ha informado esa reserva o no existe."));

        if (!reservation.getHolderCode().equals(student)) {
            throw DomainException.forbidden("NOT_RESERVATION_HOLDER", "La reserva no pertenece a este estudiante.");
        }

        if (checkouts.existsByReservationId(reservationId) || !"ACTIVA".equals(reservation.getStatus())) {
            throw DomainException.conflict("RESERVATION_ALREADY_CLOSED", "La utilización ya fue finalizada o la reserva fue cancelada.");
        }

        if (!reservation.isSanctionable() || "ACADEMICO".equalsIgnoreCase(reservation.getOrigin())) {
            throw DomainException.badRequest("RESERVATION_NOT_SANCTIONABLE", "Esta reserva no admite check-out en M3.");
        }

        if (reservation.getResourceId() == null || reservation.getHolderCode() == null) {
            throw DomainException.badRequest("RESERVATION_INCOMPLETE", "No fue posible validar la utilización informada por M2.");
        }

        Instant now = clock.instant();
        if (now.isBefore(reservation.getStartsAt())) {
            throw DomainException.conflict("RESERVATION_NOT_STARTED", "La utilización todavía no ha comenzado.");
        }

        Instant dueAt = "ACTIVO".equalsIgnoreCase(reservation.getResourceCategory())
            ? reservation.getLoanDueAt()
            : reservation.getEndsAt();

        if (dueAt == null) {
            throw DomainException.badRequest("MISSING_DUE_DATE", "La reserva no trae fecha límite; no se puede validar el check-out.");
        }

        // Crear y guardar el registro de utilización finalizada
        UsageRecord usage = new UsageRecord();
        usage.setReservationId(reservationId);
        usage.setStudentCode(student);
        usage.setResourceId(reservation.getResourceId());
        usage.setResourceName(reservation.getResourceName());
        usage.setResourceCategory(reservation.getResourceCategory());
        usage.setResourceType(reservation.getResourceType());
        usage.setResourceLocation(reservation.getResourceLocation());
        usage.setFaculty(reservation.getFaculty());
        usage.setDueAt(dueAt);
        usage.setStatus("FINALIZADA");
        usage.setStartedAt(reservation.getLoanWithdrawnAt() == null ? reservation.getStartsAt() : reservation.getLoanWithdrawnAt());
        usage = usages.save(usage);

        boolean activeAsset = "ACTIVO".equalsIgnoreCase(reservation.getResourceCategory());
        if (activeAsset && reservation.getLoanWithdrawnAt() == null) {
            throw DomainException.conflict("LOAN_NOT_WITHDRAWN", "M2 aún no informa el retiro del activo; reintenta cuando el préstamo esté en uso.");
        }

        boolean late = activeAsset && now.isAfter(dueAt);
        int delayMinutes = late ? (int) Math.min(Integer.MAX_VALUE, Math.ceil(Duration.between(dueAt, now).toMillis() / 60000.0)) : 0;

        // Crear y guardar el registro de check-out
        CheckOutRecord checkOut = new CheckOutRecord();
        checkOut.setUsageId(usage.getId());
        checkOut.setReservationId(reservationId);
        checkOut.setStudentCode(student);
        checkOut.setResourceId(reservation.getResourceId());
        checkOut.setResourceName(reservation.getResourceName());
        checkOut.setResourceCategory(reservation.getResourceCategory());
        checkOut.setResourceType(reservation.getResourceType());
        checkOut.setResourceLocation(reservation.getResourceLocation());
        checkOut.setFaculty(reservation.getFaculty());
        checkOut.setCheckedOutAt(now);
        checkOut.setResult(late ? "CON_RETRASO" : "A_TIEMPO");
        checkOut.setDelayMinutes(delayMinutes);
        checkOut.setHasDamage(hasDamage);
        checkOut.setDamageDescription(hasDamage ? description.trim() : null);
        checkOut.setAckStatus("PENDIENTE");
        checkOut.setDueAt(dueAt);
        checkouts.save(checkOut);

        reservation.setStatus("FINALIZADA");
        reservations.save(reservation);

        // Tratamiento de daño o bonificación de score
        if (hasDamage) {
            DamageReport report = new DamageReport();
            report.setUsageId(usage.getId());
            report.setReservationId(reservationId);
            report.setStudentCode(student);
            report.setResourceId(reservation.getResourceId());
            report.setResourceName(reservation.getResourceName());
            report.setResourceCategory(reservation.getResourceCategory());
            report.setResourceType(reservation.getResourceType());
            report.setResourceLocation(reservation.getResourceLocation());
            report.setFaculty(reservation.getFaculty());
            report.setReportedBy(student);
            report.setDescription(description.trim());
            report.setReportedAt(now);
            report.setIssueStatus("PENDIENTE");
            report.setChargeResult("PENDIENTE");
            damageReports.save(report);

            scores.markInfraction(student);
        } else if (!late) {
            scores.apply(
                student,
                configuration.requiredInt("CHECKOUT_ON_TIME_POINTS"),
                "AUMENTO_CHECKOUT",
                "Devolución a tiempo y sin novedad.",
                "CHECKOUT",
                reservationId
            );
        }

        // Publicación de eventos hacia M1
        Map<String, Object> m1Payload = new LinkedHashMap<>();
        m1Payload.put("reservationId", reservationId);
        m1Payload.put("resourceId", reservation.getResourceId());
        m1Payload.put("occurredAt", now.toString());
        m1Payload.put("studentCode", student);
        m1Payload.put("hasDamage", hasDamage);
        if (hasDamage) {
            m1Payload.put("description", description.trim());
        }

        if (hasDamage) {
            usagePublisher.damageReported(reservation.getResourceId(), m1Payload);
        } else {
            usagePublisher.reservationFinalized(reservation.getResourceId(), m1Payload);
        }

        // Publicación de eventos hacia M2
        Map<String, Object> m2Payload = new LinkedHashMap<>();
        m2Payload.put("reservationId", reservationId);
        m2Payload.put("occurredAt", now.toString());
        if (!activeAsset) {
            m2Payload.put("verdict", hasDamage ? "REQUIERE_MANTENIMIENTO" : "SIN_NOVEDAD");
        }
        reservationPublisher.checkOutRegistered(reservationId, m2Payload, !activeAsset);

        String message = late
            ? "Devolución registrada con retraso; los descuentos diarios previos se conservan."
            : hasDamage
                ? "Devolución registrada y novedad técnica reportada."
                : "Devolución registrada a tiempo.";

        return new CheckOutResponse(
            reservationId,
            checkOut.getResult(),
            delayMinutes,
            hasDamage,
            scores.current(student).getScore(),
            message
        );
    }
}
