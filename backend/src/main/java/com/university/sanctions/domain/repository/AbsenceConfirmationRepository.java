package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.AbsenceConfirmation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AbsenceConfirmationRepository extends JpaRepository<AbsenceConfirmation, Long> {
    Optional<AbsenceConfirmation> findByReservationId(String reservationId);
    boolean existsByReservationId(String reservationId);
    List<AbsenceConfirmation> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    List<AbsenceConfirmation> findByStudentCodeOrderByCreatedAtDesc(String studentCode);
    List<AbsenceConfirmation> findByCreatedAtBetween(Instant from, Instant to);
    long countByStudentCodeAndStatusIn(String studentCode, List<String> statuses);
    long countByStatusIn(List<String> statuses);
}