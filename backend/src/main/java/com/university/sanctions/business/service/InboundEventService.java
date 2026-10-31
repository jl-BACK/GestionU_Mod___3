package com.university.sanctions.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sanctions.domain.model.DamageReport;
import com.university.sanctions.domain.model.LostLoan;
import com.university.sanctions.domain.model.ProcessedEvent;
import com.university.sanctions.domain.model.ReservationCopy;
import com.university.sanctions.domain.model.ReservationOrder;
import com.university.sanctions.domain.model.StudentRef;
import com.university.sanctions.domain.model.TrustScore;
import com.university.sanctions.domain.repository.CheckOutRecordRepository;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.domain.repository.LostLoanRepository;
import com.university.sanctions.domain.repository.ProcessedEventRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import com.university.sanctions.domain.repository.ReservationOrderRepository;
import com.university.sanctions.domain.repository.StudentRefRepository;
import com.university.sanctions.domain.repository.TrustScoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundEventService {

    private final ObjectMapper mapper;
    private final ProcessedEventRepository processed;
    private final ReservationCopyRepository reservations;
    private final ReservationOrderRepository ordering;
    private final StudentRefRepository students;
    private final TrustScoreRepository scores;
    private final AbsenceService absences;
    private final CheckOutRecordRepository checkouts;
    private final DamageReportRepository damages;
    private final LostLoanRepository lostLoans;
    private final ChargeService charges;
    private final InconsistencyService inconsistencies;
    private final Clock clock;

    public InboundEventService(
        ObjectMapper mapper,
        ProcessedEventRepository processed,
        ReservationCopyRepository reservations,
        ReservationOrderRepository ordering,
        StudentRefRepository students,
        TrustScoreRepository scores,
        AbsenceService absences,
        CheckOutRecordRepository checkouts,
        DamageReportRepository damages,
        LostLoanRepository lostLoans,
        ChargeService charges,
        InconsistencyService inconsistencies,
        Clock clock
    ) {
        this.mapper = mapper;
        this.processed = processed;
        this.reservations = reservations;
        this.ordering = ordering;
        this.students = students;
        this.scores = scores;
        this.absences = absences;
        this.checkouts = checkouts;
        this.damages = damages;
        this.lostLoans = lostLoans;
        this.charges = charges;
        this.inconsistencies = inconsistencies;
        this.clock = clock;
    }

    @Transactional
    public void accept(String topic, String message) {
        try {
            JsonNode root = mapper.readTree(message);
            String eventId = text(root, "eventId", "event_id");
            String type = text(root, "type", "eventType", "event_type");

            if (eventId == null || type == null) {
                throw new IllegalArgumentException("El evento requiere eventId y type.");
            }
            if (processed.existsById(eventId)) {
                return;
            }

            JsonNode data = root.path("data").isObject() ? root.path("data") : root;
            String normalizedType = type.toLowerCase().replace("_", "").replace("-", "").replace(".", "");

            switch (normalizedType) {
                case "reservationrecordcreated", "reservationcreated" -> saveReservation(data, eventInstant(root, data));
                case "reservationcancelled" -> cancelReservation(data, eventInstant(root, data));
                case "loandeclaredlost" -> createLostLoan(data);
                case "noshowreportacknowledged" -> acknowledgeAbsence(data);
                case "checkoutacknowledged" -> acknowledgeCheckout(data);
                case "estadoactualizado", "resourcestateupdated" -> updateMaintenanceState(data);
                default -> throw new IllegalArgumentException("Tipo de evento M1/M2 no reconocido: " + type);
            }

            processed.save(new ProcessedEvent(eventId, topic, clock.instant()));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo procesar el evento recibido en " + topic + ": " + ex.getMessage(), ex);
        }
    }

    private void saveReservation(JsonNode d, Instant occurredAt) {
        JsonNode r = firstObject(d, "reservation", "reserva", "record");
        String id = text(r, "reservationId", "reservation_id", "id");
        String holder = text(r, "holderCode", "holder_code", "studentCode", "student_code", "personCode");

        if (id == null || holder == null) {
            throw new IllegalArgumentException("La reserva debe incluir reservationId y titular.");
        }

        ReservationCopy row = reservations.findById(id).orElseGet(ReservationCopy::new);
        ReservationOrder order = ordering.findById(id).orElse(null);

        if (order != null && (occurredAt.isBefore(order.getLastEventAt()) ||
            (occurredAt.equals(order.getLastEventAt()) && "CANCELADA".equals(order.getStatus())))) {
            return;
        }

        if (row.getReservationId() != null && row.getLastEventAt() != null && occurredAt.isBefore(row.getLastEventAt())) {
            return;
        }

        row.setReservationId(id);
        row.setHolderCode(holder);
        row.setHolderName(text(r, "holderName", "holder_name", "studentName", "personName"));
        row.setOrigin(orDefault(text(r, "origin", "source"), "ESTUDIANTE"));
        row.setSanctionable(bool(r, true, "sanctionable", "isSanctionable", "is_sanctionable"));

        String category = text(r, "resourceCategory", "resource_category", "category");
        JsonNode resource = firstObject(r, "resource", "recurso");
        row.setResourceId(or(text(r, "resourceId", "resource_id", "assetId"), text(resource, "id", "resourceId", "resource_id")));
        row.setResourceName(or(text(r, "resourceName", "resource_name"), text(resource, "name", "nombre")));
        row.setResourceCategory(normalizeCategory(or(category, text(resource, "category", "type"))));
        row.setResourceType(or(text(r, "resourceType", "resource_type"), text(resource, "type", "resourceType")));
        row.setResourceLocation(or(text(r, "resourceLocation", "resource_location"), text(resource, "location", "ubicacion")));
        row.setFaculty(text(r, "faculty", "facultyName", "facultad"));
        row.setStartsAt(instant(r, "startsAt", "starts_at", "startAt", "start_at", "startDateTime"));
        row.setEndsAt(instant(r, "endsAt", "ends_at", "endAt", "end_at", "endDateTime"));
        row.setLoanWithdrawnAt(instant(r, "loanWithdrawnAt", "loan_withdrawn_at", "withdrawnAt", "pickedUpAt"));
        row.setLoanDueAt(instant(r, "loanDueAt", "loan_due_at", "dueAt", "due_at"));
        row.setStatus(orDefault(text(r, "status"), "ACTIVA"));
        row.setLastEventAt(occurredAt);

        if (row.getResourceId() == null || row.getResourceCategory() == null || row.getStartsAt() == null || row.getEndsAt() == null) {
            throw new IllegalArgumentException("La reserva recibida no trae recurso, categoría o ventana temporal completa.");
        }
        if ("ACTIVO".equals(row.getResourceCategory()) && row.getLoanDueAt() == null) {
            throw new IllegalArgumentException("El préstamo de activo no trae fecha límite.");
        }

        reservations.save(row);

        ReservationOrder currentOrder = (order == null) ? new ReservationOrder() : order;
        currentOrder.setReservationId(id);
        currentOrder.setLastEventAt(occurredAt);
        currentOrder.setStatus(row.getStatus());
        ordering.save(currentOrder);

        students.findById(holder).orElseGet(() ->
            students.save(new StudentRef(holder, row.getHolderName() == null ? holder : row.getHolderName()))
        );

        scores.findById(holder).orElseGet(() -> {
            var score = new TrustScore(holder, 50);
            score.setLastRecoveryAt(clock.instant());
            return scores.save(score);
        });
    }

    private void cancelReservation(JsonNode d, Instant occurredAt) {
        JsonNode r = firstObject(d, "reservation", "reserva");
        String id = text(d, "reservationId", "reservation_id", "id");
        if (id == null) {
            id = text(r, "reservationId", "reservation_id", "id");
        }
        if (id == null) {
            throw new IllegalArgumentException("La cancelación debe incluir reservationId.");
        }

        ReservationCopy row = reservations.findById(id).orElse(null);
        ReservationOrder order = ordering.findById(id).orElse(null);

        if (order != null && occurredAt.isBefore(order.getLastEventAt())) {
            return;
        }

        if (row == null) {
            ReservationOrder tombstone = (order == null) ? new ReservationOrder() : order;
            tombstone.setReservationId(id);
            tombstone.setLastEventAt(occurredAt);
            tombstone.setStatus("CANCELADA");
            ordering.save(tombstone);
            inconsistencies.record("RESERVATION", id, "Llegó una cancelación antes de recibir la copia de la reserva; se conservó su orden temporal.");
            return;
        }

        if (row.getLastEventAt() == null || !occurredAt.isBefore(row.getLastEventAt())) {
            row.setStatus("CANCELADA");
            row.setLastEventAt(occurredAt);
            reservations.save(row);

            ReservationOrder currentOrder = (order == null) ? new ReservationOrder() : order;
            currentOrder.setReservationId(id);
            currentOrder.setLastEventAt(occurredAt);
            currentOrder.setStatus("CANCELADA");
            ordering.save(currentOrder);
        }
    }

    private void createLostLoan(JsonNode d) {
        String id = text(d, "reservationId", "reservation_id");
        ReservationCopy reservation = (id == null) ? null : reservations.findById(id).orElse(null);
        if (reservation == null) {
            throw new IllegalArgumentException("LoanDeclaredLost hace referencia a una reserva desconocida.");
        }

        LostLoan loan = lostLoans.findById(id).orElseGet(LostLoan::new);
        loan.setReservationId(id);
        loan.setStudentCode(reservation.getHolderCode());
        loan.setResourceId(reservation.getResourceId());
        loan.setResourceName(reservation.getResourceName());
        loan.setResourceType(reservation.getResourceType());
        loan.setDeclaredAt(clock.instant());
        loan.setDaysLate(7);
        loan.setChargeResult("PENDIENTE");
        loan = lostLoans.save(loan);

        charges.createLostLoanCharge(loan);
    }

    private void acknowledgeAbsence(JsonNode d) {
        String id = text(d, "reservationId", "reservation_id");
        String result = text(d, "result", "status", "outcome");
        if (result == null && d.has("accepted")) {
            result = d.path("accepted").asBoolean() ? "ACEPTADO" : "RECHAZADO";
        }
        if (id == null || result == null) {
            throw new IllegalArgumentException("El acuse de inasistencia requiere reservationId y resultado.");
        }

        absences.acknowledge(
            id,
            result,
            text(d, "reasonCode", "errorCode", "code", "reason", "rejectReason"),
            instant(d, "acceptedFrom", "accepted_from")
        );
    }

    private void acknowledgeCheckout(JsonNode d) {
        String id = text(d, "reservationId", "reservation_id");
        String result = text(d, "result", "status", "outcome");
        if (result == null && d.has("accepted")) {
            result = d.path("accepted").asBoolean() ? "ACEPTADO" : "RECHAZADO";
        }

        var row = checkouts.findByReservationId(id).orElse(null);
        if (row == null) {
            inconsistencies.record("CHECK_OUT", id, "M2 acusó un check-out que M3 no registra.");
            return;
        }

        String reason = text(d, "reason", "rejectReason", "code");
        if (result != null && (result.equalsIgnoreCase("ACEPTADO") || result.equalsIgnoreCase("ACCEPTED"))) {
            row.setAckStatus("ACEPTADO");
            row.setAckReason(null);
            Instant dueAt = instant(d, "dueAt", "due_at");
            if (dueAt != null && "ACTIVO".equalsIgnoreCase(row.getResourceCategory())) {
                row.setDueAt(dueAt);
            }
        } else if ("SLOT_NOT_FINISHED".equalsIgnoreCase(reason) && instant(d, "acceptedFrom", "accepted_from") != null) {
            row.setAckStatus("REINTENTO");
            row.setAcceptedFrom(instant(d, "acceptedFrom", "accepted_from"));
        } else {
            row.setAckStatus("RECHAZADO");
            row.setAckReason(reason == null ? result : reason);
            inconsistencies.record("CHECK_OUT", id, "M2 rechazó el check-out: " + row.getAckReason());
        }

        checkouts.save(row);
    }

    private void updateMaintenanceState(JsonNode d) {
        String resourceId = text(d, "resourceId", "resource_id", "id");
        String status = text(d, "status", "state", "newStatus", "new_status");
        if (resourceId == null || status == null) {
            throw new IllegalArgumentException("El evento de M1 requiere recurso y estado.");
        }

        if (!Set.of("DISPONIBLE", "AVAILABLE", "ACTIVO").contains(status.toUpperCase())) {
            return;
        }

        for (DamageReport report : damages.findByResourceIdAndIssueStatus(resourceId, "PENDIENTE")) {
            report.setIssueStatus("RESUELTA");
            damages.save(report);
            charges.resolveDamageReport(report);
        }
    }

    private static JsonNode firstObject(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name) && node.get(name).isObject()) {
                return node.get(name);
            }
        }
        return node;
    }

    private static String text(JsonNode node, String... names) {
        if (node == null) return null;
        for (String name : names) {
            if (node.hasNonNull(name) && !node.get(name).isContainerNode()) {
                String value = node.get(name).asText();
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }

    private static String or(String left, String right) {
        return left == null ? right : left;
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static boolean bool(JsonNode n, boolean fallback, String... names) {
        String v = text(n, names);
        return v == null ? fallback : Boolean.parseBoolean(v);
    }

    private static String normalizeCategory(String value) {
        if (value == null) return null;
        String v = value.toUpperCase();
        if (v.contains("SPACE") || v.contains("ESPAC")) return "ESPACIO";
        if (v.contains("ASSET") || v.contains("EQUIP") || v.contains("ACTIV")) return "ACTIVO";
        return v;
    }

    private static Instant eventInstant(JsonNode envelope, JsonNode data) {
        Instant value = instant(envelope, "occurredAt", "occurred_at");
        if (value != null) return value;
        value = instant(data, "occurredAt", "occurred_at", "cancelledAt", "cancelled_at");
        return value == null ? Instant.now() : value;
    }

    private static Instant instant(JsonNode node, String... names) {
        String value = text(node, names);
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
