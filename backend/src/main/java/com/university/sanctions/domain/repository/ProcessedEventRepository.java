package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}