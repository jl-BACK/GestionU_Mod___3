package com.university.sanctions.infrastructure.messaging;

import com.university.sanctions.business.service.InboundEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InboundKafkaListeners {
    private final InboundEventService events;
    public InboundKafkaListeners(InboundEventService events) { this.events=events; }

    @KafkaListener(topics="${app.kafka.m2-record-topic:module2.reservation.record.v1}")
    public void record(String message) { events.accept("module2.reservation.record.v1", message); }
    @KafkaListener(topics="${app.kafka.m2-cancellation-topic:module2.reservation.cancellation.v1}")
    public void cancellation(String message) { events.accept("module2.reservation.cancellation.v1", message); }
    @KafkaListener(topics="${app.kafka.m2-no-show-ack-topic:module2.reservation.no-show-ack.v1}")
    public void noShowAck(String message) { events.accept("module2.reservation.no-show-ack.v1", message); }
    @KafkaListener(topics="${app.kafka.m2-checkout-ack-topic:module2.reservation.check-out-ack.v1}")
    public void checkoutAck(String message) { events.accept("module2.reservation.check-out-ack.v1", message); }
    @KafkaListener(topics="${app.kafka.m1-state-topic:unimag.m1.recursos-estados.v1}")
    public void resourceState(String message) { events.accept("unimag.m1.recursos-estados.v1", message); }
}