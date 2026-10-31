package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.UsageRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    Optional<UsageRecord> findByReservationId(String reservationId);
    Optional<UsageRecord> findFirstByResourceIdOrderByStartedAtDesc(String resourceId);
    List<UsageRecord> findByStudentCodeAndStatusOrderByStartedAtDesc(String studentCode, String status);
}