package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.AbsenceConfirmation;
import com.university.sanctions.domain.model.DamageReport;
import com.university.sanctions.domain.repository.AbsenceConfirmationRepository;
import com.university.sanctions.domain.repository.ChargeRepository;
import com.university.sanctions.domain.repository.CheckOutRecordRepository;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.domain.repository.LostLoanRepository;
import com.university.sanctions.domain.repository.OverdueDeductionRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import com.university.sanctions.domain.repository.StudentRefRepository;
import com.university.sanctions.domain.repository.TrustScoreRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private final StudentRefRepository students;
    private final TrustScoreRepository scores;
    private final AbsenceConfirmationRepository absences;
    private final CheckOutRecordRepository checkouts;
    private final DamageReportRepository damageReports;
    private final ChargeRepository charges;
    private final OverdueDeductionRepository deductions;
    private final LostLoanRepository lostLoans;
    private final ReservationCopyRepository reservations;
    private final Clock clock;

    public AnalyticsService(StudentRefRepository students, TrustScoreRepository scores, AbsenceConfirmationRepository absences,
        CheckOutRecordRepository checkouts, DamageReportRepository damageReports, ChargeRepository charges,
        OverdueDeductionRepository deductions, LostLoanRepository lostLoans, ReservationCopyRepository reservations, Clock clock) {
        this.students=students; this.scores=scores; this.absences=absences; this.checkouts=checkouts; this.damageReports=damageReports;
        this.charges=charges; this.deductions=deductions; this.lostLoans=lostLoans; this.reservations=reservations; this.clock=clock;
    }

    public Map<String,Object> overview(LocalDate from, LocalDate to, String resourceType, String faculty) {
        LocalDate end = to == null ? LocalDate.now(clock.withZone(ZONE)) : to;
        LocalDate start = from == null ? (end.getMonthValue() <= 6 ? LocalDate.of(end.getYear(), 1, 1) : LocalDate.of(end.getYear(), 7, 1)) : from;
        Instant begin = start.atStartOfDay(ZONE).toInstant();
        Instant finish = end.plusDays(1).atStartOfDay(ZONE).toInstant().minusNanos(1);
        var checkOutRows = checkouts.findByCheckedOutAtBetween(begin, finish).stream()
            .filter(c -> matches(c.getResourceType(), c.getFaculty(), resourceType, faculty)).toList();
        var absenceRows = absences.findByCreatedAtBetween(begin, finish).stream()
            .filter(a -> "ACEPTADA".equals(a.getStatus()))
            .filter(a -> matchesReservation(a.getReservationId(), resourceType, faculty)).toList();
        var damageRows = damageReports.findByReportedAtBetween(begin, finish).stream()
            .filter(d -> matches(d.getResourceType(), d.getFaculty(), resourceType, faculty)).toList();
        var chargeRows = charges.findByCreatedAtBetween(begin, finish).stream()
            .filter(c -> matchesCharge(c, resourceType, faculty)).toList();
        var deductionRows = deductions.findByCreatedAtBetween(begin, finish).stream()
            .filter(d -> matchesReservation(d.getReservationId(), resourceType, faculty)).toList();
        var lostRows = lostLoans.findByDeclaredAtBetween(begin, finish).stream()
            .filter(l -> matchesReservation(l.getReservationId(), resourceType, faculty)).toList();

        long onTime = checkOutRows.stream().filter(c -> "A_TIEMPO".equals(c.getResult())).count();
        long late = checkOutRows.stream().filter(c -> "CON_RETRASO".equals(c.getResult())).count();
        long totalReturns = onTime + late;
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("from", start.toString()); result.put("to", end.toString());
        result.put("resourceType", resourceType == null ? "" : resourceType);
        result.put("faculty", faculty == null ? "" : faculty);
        result.put("students", students.count());
        result.put("blockedStudents", scores.findByScoreLessThanOrderByLastInfractionAtAsc(1).size());
        result.put("sanctionsApplied", absenceRows.size());
        result.put("absences", absenceRows.size());
        result.put("checkouts", totalReturns);
        result.put("onTimeReturns", onTime); result.put("lateReturns", late);
        result.put("onTimePercent", totalReturns == 0 ? 0.0 : round(onTime * 100.0 / totalReturns));
        result.put("latePercent", totalReturns == 0 ? 0.0 : round(late * 100.0 / totalReturns));
        result.put("overdueLoansWithDeductions", deductionRows.stream().map(d -> d.getReservationId()).distinct().count());
        result.put("lostLoans", lostRows.size());
        result.put("damageReports", damageRows.size());
        result.put("pendingCharges", chargeRows.stream().filter(c -> "PENDIENTE".equals(c.getStatus())).count());
        result.put("paidCharges", chargeRows.stream().filter(c -> "PAGADO".equals(c.getStatus())).count());
        result.put("annulledCharges", chargeRows.stream().filter(c -> "ANULADO".equals(c.getStatus())).count());
        result.put("pendingAmountCOP", amountFor(chargeRows, "PENDIENTE"));
        result.put("paidAmountCOP", amountFor(chargeRows, "PAGADO"));
        result.put("annulledAmountCOP", amountFor(chargeRows, "ANULADO"));
        result.put("damageRankings", damageRankings(damageRows));
        result.put("absenceRankings", absenceRankings(absenceRows));
        result.put("updatedAt", clock.instant().toString());
        return result;
    }

    public Map<String,Object> rankings(LocalDate from, LocalDate to, String resourceType, String faculty) {
        var summary = overview(from, to, resourceType, faculty);
        return Map.of("damage", summary.get("damageRankings"), "absences", summary.get("absenceRankings"));
    }
    public List<Map<String,Object>> exportRows(LocalDate from, LocalDate to, String resourceType, String faculty) {
        Map<String,Object> summary = overview(from, to, resourceType, faculty);
        List<Map<String,Object>> rows = new ArrayList<>();
        summary.forEach((key,value) -> rows.add(Map.of("indicator", key, "value", String.valueOf(value))));
        return rows;
    }

    private static BigDecimal amountFor(List<com.university.sanctions.domain.model.Charge> list, String status) {
        return list.stream().filter(c -> status.equals(c.getStatus())).map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private static boolean matches(String type, String rowFaculty, String typeFilter, String facultyFilter) {
        return matchesValue(type, typeFilter) && matchesValue(rowFaculty == null ? "Sin clasificar" : rowFaculty, facultyFilter);
    }
    private static boolean matchesValue(String value, String filter) {
        return filter == null || filter.isBlank() || (value == null ? "Sin clasificar" : value).equalsIgnoreCase(filter);
    }
    private boolean matchesReservation(String reservationId, String typeFilter, String facultyFilter) {
        if ((typeFilter == null || typeFilter.isBlank()) && (facultyFilter == null || facultyFilter.isBlank())) return true;
        var reservation = reservations.findById(reservationId).orElse(null);
        return reservation != null && matches(reservation.getResourceType(), reservation.getFaculty(), typeFilter, facultyFilter);
    }
    private boolean matchesCharge(com.university.sanctions.domain.model.Charge charge, String typeFilter, String facultyFilter) {
        if ((typeFilter == null || typeFilter.isBlank()) && (facultyFilter == null || facultyFilter.isBlank())) return true;
        if (charge.getDamageReportId() != null) {
            var report = damageReports.findById(charge.getDamageReportId()).orElse(null);
            return report != null && matches(report.getResourceType(), report.getFaculty(), typeFilter, facultyFilter);
        }
        return charge.getLostLoanReservationId() != null && matchesReservation(charge.getLostLoanReservationId(), typeFilter, facultyFilter);
    }
    private static List<Map<String,Object>> damageRankings(List<DamageReport> reports) {
        Map<String,Long> grouped = reports.stream().collect(Collectors.groupingBy(
            r -> r.getResourceName() == null ? r.getResourceType() == null ? "Sin clasificar" : r.getResourceType() : r.getResourceName(),
            Collectors.counting()));
        return grouped.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(10)
            .map(e -> Map.<String,Object>of("resource", e.getKey(), "count", e.getValue())).toList();
    }
    private static List<Map<String,Object>> absenceRankings(List<AbsenceConfirmation> rows) {
        Map<String,Long> grouped = rows.stream().collect(Collectors.groupingBy(
            a -> a.getSpaceName() == null ? a.getSpaceId() : a.getSpaceName(), Collectors.counting()));
        return grouped.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(10)
            .map(e -> Map.<String,Object>of("space", e.getKey(), "count", e.getValue())).toList();
    }
}