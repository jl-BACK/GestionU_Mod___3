package com.university.sanctions.presentation.controller;

import com.university.sanctions.business.service.DamageReportService;
import com.university.sanctions.business.service.DomainException;
import com.university.sanctions.business.service.EvidenceStorage;
import com.university.sanctions.domain.model.DamageReport;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.presentation.api.ApiModels.DamageRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/damage-reports")
public class DamageController {

    private final DamageReportService service;
    private final EvidenceStorage storage;
    private final DamageReportRepository reports;

    public DamageController(
        DamageReportService service,
        EvidenceStorage storage,
        DamageReportRepository reports
    ) {
        this.service = service;
        this.storage = storage;
        this.reports = reports;
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public DamageReport report(@Valid @RequestBody DamageRequest request, Principal principal) {
        return service.report(
            request.resourceId(),
            request.reservationId(),
            principal.getName(),
            request.description(),
            null
        );
    }

    @PostMapping(value = "/{id}/evidence", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public DamageReport evidence(
        @PathVariable long id,
        @RequestPart("file") MultipartFile file,
        Principal principal
    ) {
        var report = reports.findById(id)
            .orElseThrow(() -> DomainException.notFound("DAMAGE_REPORT_NOT_FOUND", "No existe ese reporte."));

        if (!principal.getName().equals(report.getReportedBy())) {
            throw DomainException.forbidden("NOT_REPORT_OWNER", "Solo quien registró el reporte puede adjuntar evidencia.");
        }

        report.setEvidencePath(storage.store(file));
        return reports.save(report);
    }

    @GetMapping
    @PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
    public List<DamageReport> list() {
        return reports.findAll();
    }
}
