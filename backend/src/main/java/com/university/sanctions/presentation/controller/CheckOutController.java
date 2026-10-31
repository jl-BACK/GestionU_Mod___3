package com.university.sanctions.presentation.controller;

import com.university.sanctions.business.service.CheckOutService;
import com.university.sanctions.presentation.api.ApiModels.CheckOutResponse;
import com.university.sanctions.presentation.api.ApiModels.CheckoutRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkouts")
public class CheckOutController {

    private final CheckOutService service;

    public CheckOutController(CheckOutService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public CheckOutResponse checkOut(@Valid @RequestBody CheckoutRequest request, Principal principal) {
        return service.checkOut(
            principal.getName(),
            request.reservationId(),
            request.hasDamage(),
            request.damageDescription()
        );
    }
}
