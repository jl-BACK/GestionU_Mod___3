package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.OverdueDeduction;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface OverdueDeductionRepository extends JpaRepository<OverdueDeduction, Long> {
    Optional<OverdueDeduction> findByStudentCodeAndDeductionDate(String studentCode, LocalDate date);
    List<OverdueDeduction> findByStudentCodeOrderByDeductionDateDesc(String studentCode);
    List<OverdueDeduction> findByStudentCodeAndReservationIdOrderByDeductionDateAsc(String studentCode, String reservationId);
    List<OverdueDeduction> findByCreatedAtBetween(Instant from, Instant to);
    @Query("select coalesce(sum(d.points), 0) from OverdueDeduction d where d.studentCode = :code and d.reservationId = :reservation")
    Integer sumPointsForLoan(@Param("code") String code, @Param("reservation") String reservationId);
}