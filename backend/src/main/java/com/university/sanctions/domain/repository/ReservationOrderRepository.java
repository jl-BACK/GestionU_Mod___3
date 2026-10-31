package com.university.sanctions.domain.repository;

import com.university.sanctions.domain.model.ReservationOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationOrderRepository extends JpaRepository<ReservationOrder, String> {}