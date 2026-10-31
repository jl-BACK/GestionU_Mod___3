package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.DamageReport;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {
    List<DamageReport> findByStudentCodeOrderByReportedAtDesc(String studentCode);
    List<DamageReport> findByResourceIdAndIssueStatus(String resourceId, String status);
    List<DamageReport> findByReportedAtBetween(Instant from, Instant to);
    long countByReportedAtBetween(Instant from, Instant to);
    boolean existsByUsageId(Long usageId);
}