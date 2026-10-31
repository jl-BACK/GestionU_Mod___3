package com.university.sanctions.domain.repository;
import com.university.sanctions.domain.model.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {}