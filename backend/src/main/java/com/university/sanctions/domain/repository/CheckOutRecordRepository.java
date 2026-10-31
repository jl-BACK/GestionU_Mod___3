package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.CheckOutRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CheckOutRecordRepository extends JpaRepository<CheckOutRecord, Long> {
    Optional<CheckOutRecord> findByReservationId(String reservationId);
    boolean existsByReservationId(String reservationId);
    List<CheckOutRecord> findByStudentCodeOrderByCheckedOutAtDesc(String studentCode);
    List<CheckOutRecord> findByCheckedOutAtBetween(Instant from, Instant to);
    long countByResultAndCheckedOutAtBetween(String result, Instant from, Instant to);
    long countByStudentCodeAndResult(String studentCode, String result);
    List<CheckOutRecord> findByAckStatusAndAcceptedFromBefore(String status, Instant before);
}