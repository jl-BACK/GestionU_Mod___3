package com.university.sanctions.business.service;

import com.university.sanctions.business.integration.ReservationModulePublisher;
import com.university.sanctions.domain.model.AbsenceAnnulmentAudit;
import com.university.sanctions.domain.model.AbsenceConfirmation;
import com.university.sanctions.domain.model.ReservationCopy;
import com.university.sanctions.domain.model.Sanction;
import com.university.sanctions.domain.repository.AbsenceAnnulmentAuditRepository;
import com.university.sanctions.domain.repository.AbsenceConfirmationRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import com.university.sanctions.domain.repository.SanctionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbsenceService {

    private final ReservationCopyRepository reservations;
    private final AbsenceConfirmationRepository confirmations;
    private final AbsenceAnnulmentAuditRepository audits;
    private final SanctionRepository sanctions;
    private final ReservationModulePublisher publisher;
    private final ConfigurationService configuration;
    private final ScoreService scores;
    private final Clock clock;

    public AbsenceService(
        ReservationCopyRepository reservations,
        AbsenceConfirmationRepository confirmations,
        AbsenceAnnulmentAuditRepository audits,
        SanctionRepository sanctions,
        ReservationModulePublisher publisher,
        ConfigurationService configuration,
        ScoreService scores,
        Clock clock
    ) {
        this.reservations = reservations;
        this.confirmations = confirmations;
        this.audits = audits;
        this.sanctions = sanctions;
        this.publisher = publisher;
        this.configuration = configuration;
        this.scores = scores;
        this.clock = clock;
    }

    public List<ReservationCopy> eligible() {
        Instant limit = clock.instant().minus(Duration.ofMinutes(configuration.requiredInt("ABSENCE_MINUTES")));
        List<ReservationCopy> candidates = reservations
            .findByStatusAndResourceCategoryAndSanctionableTrueAndStartsAtBeforeOrderByStartsAtAsc(
                "ACTIVA",
                "ESPACIO",
                limit
            );

        return candidates.stream()
            .filter(r -> !confirmations.existsByReservationId(r.getReservationId()))
            .toList();
    }

    @Transactional
    public AbsenceConfirmation confirm(String reservationId, String actor, String note) {
        ReservationCopy reservation = reservations.findById(reservationId)
            .orElseThrow(() -> DomainException.notFound("RESERVATION_NOT_FOUND", "No existe la reserva informada por M2."));

        if (!"ACTIVA".equals(reservation.getStatus())) {
            throw DomainException.conflict("RESERVATION_CANCELLED", "La reserva está cancelada o cerrada.");
        }
        if (!"ESPACIO".equalsIgnoreCase(reservation.getResourceCategory())) {
            throw DomainException.badRequest("NOT_A_SPACE", "La inasistencia solo aplica a reservas de espacios.");
        }
        if (!reservation.isSanctionable()) {
            throw DomainException.badRequest("NOT_SANCTIONABLE", "La reserva no admite sanción por inasistencia.");
        }
        if (confirmations.existsByReservationId(reservationId)) {
            throw DomainException.conflict("ABSENCE_ALREADY_REPORTED", "Esta reserva ya tiene una confirmación de inasistencia.");
        }

        Instant now = clock.instant();
        Instant acceptedFrom = reservation.getStartsAt().plus(Duration.ofMinutes(configuration.requiredInt("ABSENCE_MINUTES")));
        if (now.isBefore(acceptedFrom)) {
            throw DomainException.conflict("TOO_EARLY", "La inasistencia podrá confirmarse desde " + acceptedFrom + ".");
        }

        AbsenceConfirmation row = new AbsenceConfirmation();
        row.setReservationId(reservationId);
        row.setStudentCode(reservation.getHolderCode());
        row.setSpaceId(reservation.getResourceId());
        row.setSpaceName(reservation.getResourceName());
        row.setStartsAt(reservation.getStartsAt());
        row.setVerifiedAt(now);
        row.setConfirmedBy(actor);
        row.setNote(note);
        row.setStatus("PENDIENTE_ACUSE");
        row.setPointsDeducted(0);
        row.setCreatedAt(now);
        row = confirmations.save(row);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservationId);
        payload.put("verifiedAt", now.toString());
        payload.put("verifiedBy", "MODULO_3");
        payload.put("note", note == null ? "" : note);
        publisher.noShowReported(reservationId, payload);

        return row;
    }

    @Transactional
    public void acknowledge(String reservationId, String result, String reason, Instant acceptedFrom) {
        AbsenceConfirmation row = confirmations.findByReservationId(reservationId).orElse(null);
        if (row == null || !"PENDIENTE_ACUSE".equals(row.getStatus())) {
            return;
        }

        if ("ACEPTADO".equalsIgnoreCase(result) || "ACCEPTED".equalsIgnoreCase(result) || "DUPLICATE".equalsIgnoreCase(result)) {
            row.setStatus("ACEPTADA");
            row.setRejectReason(null);
            createSanction(row);
        } else {
            row.setStatus("RECHAZADA_M2");
            row.setRejectReason(reason == null ? result : reason);
            row.setAcceptedFrom(acceptedFrom);
        }

        confirmations.save(row);
    }

    private void createSanction(AbsenceConfirmation row) {
        var existing = sanctions.findFirstByAbsenceConfirmationId(row.getId()).orElse(null);
        if (existing != null) {
            return;
        }

        LocalDate today = clock.instant().atZone(ZoneId.of("America/Bogota")).toLocalDate();
        int duration = configuration.requiredInt("ABSENCE_DURATION_DAYS");

        Sanction sanction = new Sanction();
        sanction.setStudentCode(row.getStudentCode());
        sanction.setType("INASISTENCIA");
        sanction.setStatus("VIGENTE");
        sanction.setReason("Inasistencia a reserva de espacio " + row.getSpaceName());
        sanction.setStartsOn(today);
        sanction.setEndsOn(today.plusDays(Math.max(0, duration - 1L)));
        sanction.setAbsenceConfirmationId(row.getId());
        sanction.setCreatedAt(clock.instant());
        sanctions.save(sanction);

        int before = scores.current(row.getStudentCode()).getScore();
        int after = scores.apply(
            row.getStudentCode(),
            -configuration.requiredInt("SANCTION_POINTS"),
            "REDUCCION_SANCION",
            "Sanción por inasistencia a " + row.getSpaceName(),
            "SANCTION",
            String.valueOf(row.getId())
        );

        row.setPointsDeducted(Math.max(0, before - after));
        scores.markInfraction(row.getStudentCode());
    }

    @Transactional
    public AbsenceConfirmation retry(long id) {
        AbsenceConfirmation row = confirmations.findById(id)
            .orElseThrow(() -> DomainException.notFound("ABSENCE_NOT_FOUND", "No existe esa inasistencia."));

        if (!"RECHAZADA_M2".equals(row.getStatus()) || row.getRejectReason() == null ||
            !row.getRejectReason().toUpperCase().contains("TOO_EARLY") || row.getAcceptedFrom() == null) {
            throw DomainException.conflict("ABSENCE_NOT_RETRYABLE", "M2 no indicó que esta inasistencia pueda reintentarse.");
        }

        if (clock.instant().isBefore(row.getAcceptedFrom())) {
            throw DomainException.conflict("TOO_EARLY", "Aún no llegó el momento indicado por M2.");
        }

        row.setStatus("PENDIENTE_ACUSE");
        row.setRejectReason(null);
        row.setAcceptedFrom(null);
        row = confirmations.save(row);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", row.getReservationId());
        payload.put("verifiedAt", row.getVerifiedAt().toString());
        payload.put("verifiedBy", "MODULO_3");
        payload.put("note", row.getNote() == null ? "" : row.getNote());
        publisher.noShowReported(row.getReservationId(), payload);

        return row;
    }

    @Transactional
    public AbsenceConfirmation annul(long id, String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw DomainException.badRequest("REASON_REQUIRED", "El motivo de anulación es obligatorio.");
        }

        AbsenceConfirmation row = confirmations.findById(id)
            .orElseThrow(() -> DomainException.notFound("ABSENCE_NOT_FOUND", "No existe esa inasistencia."));

        if ("ANULADA".equals(row.getStatus())) {
            throw DomainException.conflict("ABSENCE_ALREADY_ANNULLED", "La inasistencia ya fue anulada.");
        }

        boolean accepted = "ACEPTADA".equals(row.getStatus());
        row.setStatus("ANULADA");

        int restored = 0;
        if (accepted) {
            Sanction sanction = sanctions.findFirstByAbsenceConfirmationId(id)
                .orElseThrow(() -> DomainException.conflict("SANCTION_NOT_FOUND", "No se encontró la sanción asociada."));
            sanction.setStatus("ANULADA");
            sanction.setLiftedReason(reason);
            sanctions.save(sanction);

            int requested = Math.abs(scores.effectivePoints(row.getStudentCode(), "REDUCCION_SANCION", "SANCTION", String.valueOf(id)));
            int before = scores.current(row.getStudentCode()).getScore();
            int after = scores.restore(row.getStudentCode(), requested, reason, "ABSENCE_ANNULMENT", String.valueOf(id));
            restored = Math.max(0, after - before);
        }

        confirmations.save(row);

        AbsenceAnnulmentAudit audit = new AbsenceAnnulmentAudit();
        audit.setConfirmationId(id);
        audit.setAnnulledBy(actor);
        audit.setAnnulledAt(clock.instant());
        audit.setReason(reason);
        audit.setPointsRestored(restored);
        audits.save(audit);

        if (accepted) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reservationId", row.getReservationId());
            payload.put("voidedReason", reason);
            publisher.noShowReportVoided(row.getReservationId(), payload);
        }

        return row;
    }

    public List<AbsenceConfirmation> recent() {
        return confirmations.findByStatusInOrderByCreatedAtDesc(
            List.of("PENDIENTE_ACUSE", "RECHAZADA_M2", "ACEPTADA")
        );
    }

    public Map<String, Object> acknowledgementView() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", recent());
        result.put("eligible", eligible());
        result.put("pendingAcknowledgements", confirmations.countByStatusIn(List.of("PENDIENTE_ACUSE")));
        return result;
    }
}
