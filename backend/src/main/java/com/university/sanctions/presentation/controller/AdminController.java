package com.university.sanctions.presentation.controller;

import com.university.sanctions.business.service.AnalyticsService;
import com.university.sanctions.business.service.ChargeService;
import com.university.sanctions.business.service.ConfigurationService;
import com.university.sanctions.business.service.UnblockService;
import com.university.sanctions.domain.model.Charge;
import com.university.sanctions.domain.model.CostCatalog;
import com.university.sanctions.domain.model.SystemConfiguration;
import com.university.sanctions.domain.repository.ChargeRepository;
import com.university.sanctions.domain.repository.CostCatalogRepository;
import com.university.sanctions.presentation.api.ApiModels.ConfigRequest;
import com.university.sanctions.presentation.api.ApiModels.CostRequest;
import com.university.sanctions.presentation.api.ApiModels.ReasonRequest;
import com.university.sanctions.presentation.api.ApiModels.UnblockResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PERSONAL_ADMINISTRATIVO')")
public class AdminController {

    private final AnalyticsService analytics;
    private final UnblockService unblocks;
    private final ChargeService chargeService;
    private final ChargeRepository charges;
    private final CostCatalogRepository costs;
    private final ConfigurationService configuration;

    public AdminController(
        AnalyticsService analytics,
        UnblockService unblocks,
        ChargeService chargeService,
        ChargeRepository charges,
        CostCatalogRepository costs,
        ConfigurationService configuration
    ) {
        this.analytics = analytics;
        this.unblocks = unblocks;
        this.chargeService = chargeService;
        this.charges = charges;
        this.costs = costs;
        this.configuration = configuration;
    }

    @PostMapping("/students/{code}/unblock")
    public UnblockResponse unblock(
        @PathVariable String code,
        @Valid @RequestBody ReasonRequest request,
        Principal principal
    ) {
        return unblocks.unblock(code, principal.getName(), request.reason());
    }

    @GetMapping("/analytics/overview")
    public Map<String, Object> overview(
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String faculty
    ) {
        return analytics.overview(from, to, resourceType, faculty);
    }

    @GetMapping("/analytics/rankings")
    public Map<String, Object> rankings(
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String faculty
    ) {
        return analytics.rankings(from, to, resourceType, faculty);
    }

    @GetMapping(value = "/analytics/export", produces = "text/csv")
    public ResponseEntity<String> export(
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) String resourceType,
        @RequestParam(required = false) String faculty
    ) {
        StringBuilder csv = new StringBuilder("indicador,valor\n");
        for (Map<String, Object> row : analytics.exportRows(from, to, resourceType, faculty)) {
            csv.append(csvEscape(row.get("indicator"))).append(',').append(csvEscape(row.get("value"))).append('\n');
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gestion-unimag-analytics.csv")
            .contentType(new MediaType("text", "csv"))
            .body(csv.toString());
    }

    @GetMapping("/charges")
    public List<Charge> allCharges() {
        return charges.findAll();
    }

    @PostMapping("/charges/{id}/paid")
    public Charge paid(@PathVariable long id, Principal principal) {
        return chargeService.changeStatus(id, "PAGADO", null, principal.getName());
    }

    @PostMapping("/charges/{id}/annul")
    public Charge annulCharge(
        @PathVariable long id,
        @Valid @RequestBody ReasonRequest request,
        Principal principal
    ) {
        return chargeService.changeStatus(id, "ANULADO", request.reason(), principal.getName());
    }

    @GetMapping("/cost-catalog")
    public List<CostCatalog> costCatalog() {
        return costs.findAllByOrderByResourceTypeAscIdDesc();
    }

    @PostMapping("/cost-catalog")
    public CostCatalog cost(@Valid @RequestBody CostRequest request) {
        return chargeService.upsertCost(request.resourceType(), request.amount(), request.active());
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return configuration.all();
    }

    @PutMapping("/config/{key}")
    public SystemConfiguration setConfig(
        @PathVariable String key,
        @Valid @RequestBody ConfigRequest request
    ) {
        return configuration.set(key, request.value(), request.description());
    }

    private static String csvEscape(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
