package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.TrustScore;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface TrustScoreRepository extends JpaRepository<TrustScore, String> {
    List<TrustScore> findByScoreLessThanOrderByLastInfractionAtAsc(int maxScore);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TrustScore s where s.studentCode = :code")
    Optional<TrustScore> findLocked(@Param("code") String code);
}