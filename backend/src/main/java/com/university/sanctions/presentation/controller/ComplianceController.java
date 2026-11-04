package com.university.sanctions.presentation.controller;

import com.university.sanctions.business.service.ComplianceQueryService;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ComplianceController {

    private final ComplianceQueryService queries;

    public ComplianceController(ComplianceQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/compliance/people/{code}")
    public Map<String, Object> forModule2(@PathVariable String code) {
        return queries.forModule2(code);
    }

    @GetMapping("/me/compliance")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public Map<String, Object> mine(Principal principal) {
        return queries.forStudent(principal.getName());
    }

    @GetMapping("/admin/students/{code}")
    @PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
    public Map<String, Object> student(@PathVariable String code) {
        return queries.forStudent(code);
    }
}
