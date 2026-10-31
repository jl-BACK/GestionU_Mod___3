package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.CostCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CostCatalogRepository extends JpaRepository<CostCatalog, Long> {
    Optional<CostCatalog> findFirstByResourceTypeIgnoreCaseAndActiveTrueOrderByIdDesc(String resourceType);
    List<CostCatalog> findAllByOrderByResourceTypeAscIdDesc();
}