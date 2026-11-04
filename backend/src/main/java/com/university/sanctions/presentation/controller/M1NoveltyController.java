package com.university.sanctions.presentation.controller;

import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.presentation.api.ApiModels.NoveltyPendingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/novedades")
public class M1NoveltyController {

    private final DamageReportRepository reports;

    public M1NoveltyController(DamageReportRepository reports) {
        this.reports = reports;
    }

    @GetMapping("/pendientes")
    public NoveltyPendingResponse pending(@RequestParam String recursoId) {
        boolean pending = !reports.findByResourceIdAndIssueStatus(recursoId, "PENDIENTE").isEmpty();
        return new NoveltyPendingResponse(recursoId, pending);
    }
}
