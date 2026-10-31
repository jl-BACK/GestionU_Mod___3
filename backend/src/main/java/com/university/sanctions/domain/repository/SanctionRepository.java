package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.Sanction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SanctionRepository extends JpaRepository<Sanction, Long> {
    List<Sanction> findByStudentCodeAndStatusOrderByStartsOnDesc(String studentCode, String status);
    List<Sanction> findByStudentCodeOrderByStartsOnDesc(String studentCode);
    List<Sanction> findByStudentCodeAndStatusInOrderByStartsOnDesc(String studentCode, List<String> statuses);
    List<Sanction> findByStatusAndEndsOnLessThanEqual(String status, LocalDate date);
    long countByStatusAndCreatedAtBetween(String status, java.time.Instant from, java.time.Instant to);
    Optional<Sanction> findFirstByStudentCodeOrderByCreatedAtDesc(String studentCode);
    Optional<Sanction> findFirstByAbsenceConfirmationId(Long absenceConfirmationId);
    long countByStatusAndStartsOnLessThanEqualAndEndsOnGreaterThanEqual(String status, LocalDate startsBefore, LocalDate endsAfter);
}