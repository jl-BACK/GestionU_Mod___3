package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.UnblockAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UnblockAuditRepository extends JpaRepository<UnblockAudit, Long> {
    List<UnblockAudit> findByStudentCodeOrderByUnblockedAtDesc(String studentCode);
}