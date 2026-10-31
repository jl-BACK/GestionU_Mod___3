package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.ChargeAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChargeAuditRepository extends JpaRepository<ChargeAudit, Long> {
    List<ChargeAudit> findByChargeIdOrderByChangedAtDesc(Long chargeId);
}