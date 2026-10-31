package com.university.sanctions.business.integration;

import java.util.Map;

public interface UsageEventsPublisher {
    void reservationFinalized(String resourceId, Map<String, Object> data);
    void damageReported(String resourceId, Map<String, Object> data);
}