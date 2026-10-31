package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.StudentRef;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentRefRepository extends JpaRepository<StudentRef, String> {}