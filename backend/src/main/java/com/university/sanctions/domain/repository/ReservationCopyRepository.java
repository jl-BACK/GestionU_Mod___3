package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.ReservationCopy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReservationCopyRepository extends JpaRepository<ReservationCopy, String> {
    List<ReservationCopy> findByHolderCodeAndStatusAndSanctionableTrueAndStartsAtLessThanEqualOrderByStartsAtDesc(String holderCode, String status, Instant now);
    List<ReservationCopy> findByStatusAndResourceCategoryAndLoanDueAtBeforeOrderByLoanDueAtAsc(String status, String category, Instant before);
    List<ReservationCopy> findByStatusAndResourceCategoryAndSanctionableTrueAndStartsAtBeforeOrderByStartsAtAsc(String status, String category, Instant before);
    Optional<ReservationCopy> findFirstByResourceIdOrderByStartsAtDesc(String resourceId);
}