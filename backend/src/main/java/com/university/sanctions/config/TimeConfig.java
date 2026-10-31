package com.university.sanctions.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    public static final ZoneId UNIVERSITY_ZONE = ZoneId.of("America/Bogota");

    @Bean
    Clock universityClock() {
        return Clock.system(UNIVERSITY_ZONE);
    }
}