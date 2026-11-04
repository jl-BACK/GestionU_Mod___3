package com.university.sanctions.presentation.controller;

import com.university.sanctions.business.service.AbsenceService;
import com.university.sanctions.domain.model.AbsenceConfirmation;
import com.university.sanctions.domain.model.ReservationCopy;
import com.university.sanctions.presentation.api.ApiModels.AbsenceRequest;
import com.university.sanctions.presentation.api.ApiModels.ReasonRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/absences")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class AbsenceController {

    private final AbsenceService service;

    public AbsenceController(AbsenceService service) {
        this.service = service;
    }

    @GetMapping("/eligible")
    public List<ReservationCopy> eligible() {
        return service.eligible();
    }

    @GetMapping
    public List<AbsenceConfirmation> recent() {
        return service.recent();
    }

    @PostMapping
    public AbsenceConfirmation confirm(@Valid @RequestBody AbsenceRequest request, Principal principal) {
        return service.confirm(request.reservationId(), principal.getName(), request.note());
    }

    @PostMapping("/{id}/retry")
    public AbsenceConfirmation retry(@PathVariable long id) {
        return service.retry(id);
    }

    @PostMapping("/{id}/annul")
    public AbsenceConfirmation annul(
        @PathVariable long id,
        @Valid @RequestBody ReasonRequest request,
        Principal principal
    ) {
        return service.annul(id, principal.getName(), request.reason());
    }
}
