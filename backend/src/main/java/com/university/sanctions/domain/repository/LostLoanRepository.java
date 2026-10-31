package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.LostLoan;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LostLoanRepository extends JpaRepository<LostLoan, String> {
    List<LostLoan> findByStudentCodeOrderByDeclaredAtDesc(String studentCode);
    List<LostLoan> findByDeclaredAtBetween(Instant from, Instant to);
}