package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.ScoreHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, Long> {
    Optional<ScoreHistory> findFirstByStudentCodeAndTypeAndOriginTypeAndOriginId(String studentCode, String type, String originType, String originId);
    Page<ScoreHistory> findByStudentCodeOrderByCreatedAtDesc(String studentCode, Pageable pageable);
    List<ScoreHistory> findByStudentCodeOrderByCreatedAtDesc(String studentCode);
    List<ScoreHistory> findByStudentCodeAndTypeAndOriginTypeAndOriginId(String studentCode, String type, String originType, String originId);
}