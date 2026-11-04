package com.university.sanctions.infrastructure.messaging.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sanctions.domain.model.OutboxMessage;
import com.university.sanctions.domain.repository.OutboxMessageRepository;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxWorker {
    private final OutboxMessageRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final OutboxService outboxService;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final int batchSize;

    public OutboxWorker(OutboxMessageRepository repository, KafkaTemplate<String, String> kafka,
        OutboxService outboxService, ObjectMapper mapper, Clock clock,
        @Value("${app.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.kafka = kafka;
        this.outboxService = outboxService;
        this.mapper = mapper;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:700}")
    public void publishPending() {
        var pending = repository.findTop50ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc("PENDIENTE", clock.instant());
        for (OutboxMessage event : pending.stream().limit(batchSize).toList()) {
            try {
                RecordHeaders headers = new RecordHeaders();
                if (event.getHeadersJson() != null) {
                    Map<String, String> values = mapper.readValue(event.getHeadersJson(), new TypeReference<>() {});
                    values.forEach((key, value) -> headers.add(key, value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                }
                ProducerRecord<String, String> record = new ProducerRecord<>(
                    event.getTopic(), null, event.getMessageKey(), event.getPayload(), headers);
                kafka.send(record).get(5, TimeUnit.SECONDS);
                event.setStatus("PUBLICADO");
                event.setPublishedAt(clock.instant());
                event.setLastError(null);
                repository.save(event);
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                int seconds = Math.min(300, 1 << Math.min(8, event.getAttempts()));
                event.setNextRetryAt(clock.instant().plusSeconds(seconds));
                String message = ex.getMessage() == null ? "Error de publicación Kafka" : ex.getMessage();
                event.setLastError(message.substring(0, Math.min(1000, message.length())));
                repository.save(event);
            }
        }
    }
}