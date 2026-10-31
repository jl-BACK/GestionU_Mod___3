package com.university.sanctions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestionUnimagApplication {
    public static void main(String[] args) {
        SpringApplication.run(GestionUnimagApplication.class, args);
    }
}