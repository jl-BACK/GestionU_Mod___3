package com.university.sanctions.presentation.api;

import com.university.sanctions.business.service.DomainException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<Map<String, Object>> handleDomain(DomainException ex) {
        return ResponseEntity.status(ex.status()).body(body(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst().map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("La solicitud no es válida.");
        return ResponseEntity.badRequest().body(body("VALIDATION_ERROR", message));
    }

    private Map<String, Object> body(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("code", code);
        result.put("message", message);
        return result;
    }
}