package com.university.sanctions.infrastructure.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sanctions.domain.model.OutboxMessage;
import com.university.sanctions.domain.repository.OutboxMessageRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private static final String M1_TOPIC = "unimag.m3.uso-novedades.v1";
    private final OutboxMessageRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    public OutboxService(OutboxMessageRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public String enqueueM2(String topic, String type, String key, Map<String, Object> data) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("type", type);
        envelope.put("version", 1);
        envelope.put("occurredAt", clock.instant().toString());
        envelope.put("data", data);
        return save(topic, type, key, envelope, null);
    }

    public String enqueueM1(String type, String resourceId, Map<String, Object> data) {
        String correlation = UUID.randomUUID().toString();
        Map<String, String> headers = Map.of(
            "X-Correlation-Id", correlation,
            "X-Event-Type", type,
            "X-Source-Module", "M3",
            "X-Event-Version", "1");
        return save(M1_TOPIC, type, resourceId, data, headers);
    }

    private String save(String topic, String type, String key, Object payload, Map<String, String> headers) {
        try {
            OutboxMessage message = new OutboxMessage();
            message.setId(UUID.randomUUID().toString());
            message.setTopic(topic);
            message.setEventType(type);
            message.setMessageKey(key == null ? "unknown" : key);
            message.setPayload(mapper.writeValueAsString(payload));
            message.setHeadersJson(headers == null ? null : mapper.writeValueAsString(headers));
            message.setStatus("PENDIENTE");
            message.setAttempts(0);
            message.setNextRetryAt(clock.instant());
            message.setCreatedAt(clock.instant());
            repository.save(message);
            return message.getId();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible serializar el evento del outbox.", ex);
        }
    }

    public boolean isModule1Topic(String topic) {
        return M1_TOPIC.equals(topic);
    }
}