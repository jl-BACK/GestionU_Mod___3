package com.university.sanctions.business.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoSeedRunner {
    @Bean
    ApplicationRunner demoSeed(ScoreService scores,
        @Value("${M3_DEMO_STUDENT_CODE:202610001}") String studentCode,
        @Value("${M3_DEMO_SEED:true}") boolean enabled) {
        return args -> { if (enabled) scores.ensure(studentCode, "Estudiante de demostración"); };
    }
}