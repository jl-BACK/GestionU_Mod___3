package com.university.sanctions.presentation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class ApiModels {

    private ApiModels() {}

    // --- Peticiones (Requests) ---

    public record LoginRequest(
        @NotBlank String code,
        @NotBlank String password
    ) {}

    public record CheckoutRequest(
        @NotBlank String reservationId,
        boolean hasDamage,
        @Size(max = 2000) String damageDescription
    ) {}

    public record DamageRequest(
        @NotBlank String resourceId,
        String reservationId,
        @NotBlank @Size(max = 2000) String description
    ) {}

    public record AbsenceRequest(
        @NotBlank String reservationId,
        @Size(max = 1000) String note
    ) {}

    public record ReasonRequest(
        @NotBlank @Size(max = 1000) String reason
    ) {}

    public record CostRequest(
        @NotBlank String resourceType,
        BigDecimal amount,
        boolean active
    ) {}

    public record ConfigRequest(
        @NotBlank @Size(max = 500) String value,
        @NotBlank @Size(max = 500) String description
    ) {}

    // --- Respuestas (Responses Fuertemente Tipadas) ---

    public record LoginResponse(
        String accessToken,
        String tokenType,
        String expiresAt,
        String role,
        String code
    ) {}

    public record CheckOutResponse(
        String reservationId,
        String result,
        int delayMinutes,
        boolean hasDamage,
        int score,
        String message
    ) {}

    public record UnblockResponse(
        String studentCode,
        int scoreBefore,
        int scoreAfter,
        int sanctionsLifted
    ) {}

    public record NoveltyPendingResponse(
        String recursoId,
        boolean tieneNovedadPendiente
    ) {}
}
