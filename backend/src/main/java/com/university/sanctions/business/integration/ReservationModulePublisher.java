package com.university.sanctions.business.integration;

import java.util.Map;

public interface ReservationModulePublisher {
    void checkOutRegistered(String reservationId, Map<String, Object> data, boolean space);
    void noShowReported(String reservationId, Map<String, Object> data);
    void noShowReportVoided(String reservationId, Map<String, Object> data);
}