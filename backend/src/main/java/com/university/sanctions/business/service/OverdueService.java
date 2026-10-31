package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.OverdueDeduction;
import com.university.sanctions.domain.repository.OverdueDeductionRepository;
import com.university.sanctions.domain.repository.ReservationCopyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverdueService {
    private final ReservationCopyRepository reservations;
    private final OverdueDeductionRepository deductions;
    private final ScoreService scores;
    private final Clock clock;

    public OverdueService(ReservationCopyRepository reservations, OverdueDeductionRepository deductions,
        ScoreService scores, Clock clock) {
        this.reservations = reservations; this.deductions = deductions; this.scores = scores; this.clock = clock;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "America/Bogota")
    @Transactional
    public void applyDailyDeductions() {
        var now = clock.instant();
        LocalDate today = now.atZone(java.time.ZoneId.of("America/Bogota")).toLocalDate();
        var overdue = reservations.findByStatusAndResourceCategoryAndLoanDueAtBeforeOrderByLoanDueAtAsc(
            "ACTIVA", "ACTIVO", now);
        Set<String> processedStudents = new HashSet<>();
        for (var loan : overdue) {
            String student = loan.getHolderCode();
            if (!processedStudents.add(student)) continue;
            if (deductions.findByStudentCodeAndDeductionDate(student, today).isPresent()) continue;
            long days = Duration.between(loan.getLoanDueAt(), now).toDays();
            if (days < 1) continue;
            int day = (int)Math.min(7, days);
            int base = loan.getScoreAtDue() == null ? scores.current(student).getScore() : loan.getScoreAtDue();
            if (loan.getScoreAtDue() == null) { loan.setScoreAtDue(base); reservations.save(loan); }
            Integer already = deductions.sumPointsForLoan(student, loan.getReservationId());
            int desiredTotal = day >= 7 ? base : (int)Math.ceil(base / 7.0) * day;
            int requested = Math.max(0, desiredTotal - (already == null ? 0 : already));
            int before = scores.current(student).getScore();
            int after = scores.apply(student, -requested, "REDUCCION_RETRASO",
                "Descuento por el día " + day + " de retraso del préstamo " + loan.getReservationId() + ".",
                "OVERDUE_DAY", loan.getReservationId() + ":" + today);
            int effective = Math.max(0, before - after);
            OverdueDeduction deduction = new OverdueDeduction();
            deduction.setReservationId(loan.getReservationId()); deduction.setStudentCode(student);
            deduction.setDeductionDate(today); deduction.setPoints(effective); deduction.setCreatedAt(now);
            deductions.save(deduction);
        }
    }
}
