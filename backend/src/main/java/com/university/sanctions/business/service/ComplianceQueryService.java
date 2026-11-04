package com.university.sanctions.business.service;

import com.university.sanctions.domain.repository.AbsenceConfirmationRepository;
import com.university.sanctions.domain.repository.ChargeRepository;
import com.university.sanctions.domain.repository.CheckOutRecordRepository;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.domain.repository.LostLoanRepository;
import com.university.sanctions.domain.repository.SanctionRepository;
import com.university.sanctions.domain.repository.ScoreHistoryRepository;
import com.university.sanctions.domain.repository.StudentRefRepository;
import com.university.sanctions.domain.repository.TrustScoreRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ComplianceQueryService {
    private final StudentRefRepository students;
    private final TrustScoreRepository scores;
    private final SanctionRepository sanctions;
    private final AbsenceConfirmationRepository absences;
    private final CheckOutRecordRepository checkouts;
    private final ChargeRepository charges;
    private final DamageReportRepository damageReports;
    private final ScoreHistoryRepository history;
    private final LostLoanRepository lostLoans;
    private final Clock clock;

    public ComplianceQueryService(StudentRefRepository students, TrustScoreRepository scores, SanctionRepository sanctions,
        AbsenceConfirmationRepository absences, CheckOutRecordRepository checkouts, ChargeRepository charges,
        DamageReportRepository damageReports, ScoreHistoryRepository history, LostLoanRepository lostLoans, Clock clock) {
        this.students=students; this.scores=scores; this.sanctions=sanctions; this.absences=absences; this.checkouts=checkouts;
        this.charges=charges; this.damageReports=damageReports; this.history=history; this.lostLoans=lostLoans; this.clock=clock;
    }

    public Map<String,Object> forModule2(String code) {
        var score = scores.findById(code).orElse(null);
        LocalDate today = clock.instant().atZone(java.time.ZoneId.of("America/Bogota")).toLocalDate();
        List<Map<String,Object>> active = new ArrayList<>();
        if (score != null && score.getScore() == 0) {
            active.add(Map.of("reason", "Usuario bloqueado por score en cero", "startsOn", today.toString(), "scope", "TODO"));
        }
        for (var sanction : sanctions.findByStudentCodeAndStatusInOrderByStartsOnDesc(code, List.of("VIGENTE"))) {
            if (sanction.getEndsOn().isBefore(today)) continue;
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("reason", sanction.getReason()); item.put("startsOn", sanction.getStartsOn().toString());
            item.put("endsOn", sanction.getEndsOn().toString());
            item.put("scope", "INASISTENCIA".equals(sanction.getType()) ? "ESPACIOS" : "TODO");
            active.add(item);
        }
        return Map.of("studentCode", code, "activeSanctions", active,
            "absenceCount", absences.countByStudentCodeAndStatusIn(code, List.of("ACEPTADA")),
            "lateReturns", checkouts.countByStudentCodeAndResult(code, "CON_RETRASO"), "generatedAt", clock.instant().toString());
    }

    public Map<String,Object> forStudent(String code) {
        var student = students.findById(code).orElse(null);
        var score = scores.findById(code).orElse(null);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("studentCode", code);
        result.put("name", student == null ? code : student.getName());
        result.put("score", score == null ? 50 : score.getScore());
        result.put("blocked", score != null && score.getScore() == 0);
        result.put("activeSanctions", forModule2(code).get("activeSanctions"));
        result.put("scoreHistory", history.findByStudentCodeOrderByCreatedAtDesc(code));
        result.put("absences", absences.findByStudentCodeOrderByCreatedAtDesc(code));
        result.put("checkouts", checkouts.findByStudentCodeOrderByCheckedOutAtDesc(code));
        result.put("damageReports", damageReports.findByStudentCodeOrderByReportedAtDesc(code));
        result.put("charges", charges.findByStudentCodeOrderByCreatedAtDesc(code));
        result.put("lostLoans", lostLoans.findByStudentCodeOrderByDeclaredAtDesc(code));
        return result;
    }
}