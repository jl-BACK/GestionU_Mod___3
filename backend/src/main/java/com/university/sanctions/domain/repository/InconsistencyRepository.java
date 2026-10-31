package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.Inconsistency;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InconsistencyRepository extends JpaRepository<Inconsistency, Long> {
    List<Inconsistency> findTop100ByStatusOrderByCreatedAtAsc(String status);
}