package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.Charge;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByStudentCodeOrderByCreatedAtDesc(String studentCode);
    List<Charge> findByCreatedAtBetween(Instant from, Instant to);
    long countByStatusAndCreatedAtBetween(String status, Instant from, Instant to);
    List<Charge> findByStatusAndCreatedAtBetween(String status, Instant from, Instant to);
    Optional<Charge> findFirstByDamageReportId(Long reportId);
    Optional<Charge> findFirstByLostLoanReservationId(String reservationId);
}