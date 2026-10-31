package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.OutboxMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, String> {
    List<OutboxMessage> findTop50ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(String status, Instant now);
    long countByStatus(String status);
}