package com.university.sanctions.presentation.controller;

import com.university.sanctions.business.service.DomainException;
import com.university.sanctions.presentation.api.ApiModels.LoginRequest;
import com.university.sanctions.presentation.api.ApiModels.LoginResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String studentCode;
    private final String studentPassword;
    private final String adminCode;
    private final String adminPassword;

    public AuthController(
        JwtEncoder encoder,
        Clock clock,
        @Value("${M3_DEMO_STUDENT_CODE:202610001}") String studentCode,
        @Value("${M3_DEMO_STUDENT_PASSWORD:Estudiante123!}") String studentPassword,
        @Value("${M3_DEMO_ADMIN_CODE:admin}") String adminCode,
        @Value("${M3_DEMO_ADMIN_PASSWORD:Admin123!}") String adminPassword
    ) {
        this.encoder = encoder;
        this.clock = clock;
        this.studentCode = studentCode;
        this.studentPassword = studentPassword;
        this.adminCode = adminCode;
        this.adminPassword = adminPassword;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String role;
        if (matches(request.code(), studentCode) && matches(request.password(), studentPassword)) {
            role = "ESTUDIANTE";
        } else if (matches(request.code(), adminCode) && matches(request.password(), adminPassword)) {
            role = "PERSONAL_ADMINISTRATIVO";
        } else {
            throw DomainException.forbidden("INVALID_CREDENTIALS", "Código o contraseña incorrectos.");
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(Duration.ofHours(8));

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("gestion-unimag-m3")
            .subject(request.code())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("role", role)
            .build();

        var token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
            claims
        ));

        return new LoginResponse(
            token.getTokenValue(),
            "Bearer",
            expiresAt.toString(),
            role,
            request.code()
        );
    }

    private boolean matches(String supplied, String expected) {
        if (supplied == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
            supplied.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}
