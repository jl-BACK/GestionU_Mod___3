package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.Charge;
import com.university.sanctions.domain.model.ChargeAudit;
import com.university.sanctions.domain.model.CostCatalog;
import com.university.sanctions.domain.model.DamageReport;
import com.university.sanctions.domain.model.LostLoan;
import com.university.sanctions.domain.repository.ChargeAuditRepository;
import com.university.sanctions.domain.repository.ChargeRepository;
import com.university.sanctions.domain.repository.CostCatalogRepository;
import com.university.sanctions.domain.repository.DamageReportRepository;
import com.university.sanctions.domain.repository.LostLoanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargeService {

    private final ChargeRepository charges;
    private final ChargeAuditRepository audits;
    private final CostCatalogRepository costs;
    private final DamageReportRepository reports;
    private final LostLoanRepository lostLoans;
    private final ConfigurationService configuration;
    private final Clock clock;

    public ChargeService(
        ChargeRepository charges,
        ChargeAuditRepository audits,
        CostCatalogRepository costs,
        DamageReportRepository reports,
        LostLoanRepository lostLoans,
        ConfigurationService configuration,
        Clock clock
    ) {
        this.charges = charges;
        this.audits = audits;
        this.costs = costs;
        this.reports = reports;
        this.lostLoans = lostLoans;
        this.configuration = configuration;
        this.clock = clock;
    }

    @Transactional
    public void resolveDamageReport(DamageReport report) {
        if ("GENERADO".equals(report.getChargeResult()) ||
            "SIN_COSTO".equals(report.getChargeResult()) ||
            "SIN_RESPONSABLE".equals(report.getChargeResult())) {
            return;
        }

        if (report.getStudentCode() == null || report.getStudentCode().isBlank()) {
            report.setChargeResult("SIN_RESPONSABLE");
            reports.save(report);
            return;
        }

        CostCatalog cost = costs
            .findFirstByResourceTypeIgnoreCaseAndActiveTrueOrderByIdDesc(
                report.getResourceType() == null ? "" : report.getResourceType()
            )
            .orElse(null);

        if (cost == null || cost.getAmount() == null || cost.getAmount().signum() <= 0) {
            report.setChargeResult("SIN_COSTO");
            reports.save(report);
            return;
        }

        if (charges.findFirstByDamageReportId(report.getId()).isPresent()) {
            return;
        }

        Charge charge = new Charge();
        charge.setDamageReportId(report.getId());
        charge.setStudentCode(report.getStudentCode());
        charge.setResourceId(report.getResourceId());
        charge.setResourceName(report.getResourceName());
        charge.setReason("Daño reportado: " + report.getDescription().substring(0, Math.min(150, report.getDescription().length())));
        charge.setAmount(cost.getAmount());
        charge.setStatus("PENDIENTE");
        charge.setCreatedAt(clock.instant());
        charges.save(charge);

        report.setChargeResult("GENERADO");
        reports.save(report);
    }

    @Transactional
    public void createLostLoanCharge(LostLoan loan) {
        if (charges.findFirstByLostLoanReservationId(loan.getReservationId()).isPresent()) {
            return;
        }

        CostCatalog cost = costs
            .findFirstByResourceTypeIgnoreCaseAndActiveTrueOrderByIdDesc(
                loan.getResourceType() == null ? "" : loan.getResourceType()
            )
            .orElse(null);

        if (cost == null || cost.getAmount().signum() <= 0) {
            loan.setChargeResult("SIN_COSTO");
            lostLoans.save(loan);
            return;
        }

        BigDecimal percentage = configuration.requiredDecimal("LOST_LOAN_PERCENT");
        BigDecimal amount = cost.getAmount()
            .multiply(percentage)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Charge charge = new Charge();
        charge.setLostLoanReservationId(loan.getReservationId());
        charge.setStudentCode(loan.getStudentCode());
        charge.setResourceId(loan.getResourceId());
        charge.setResourceName(loan.getResourceName());
        charge.setReason("Préstamo perdido");
        charge.setAmount(amount);
        charge.setStatus("PENDIENTE");
        charge.setCreatedAt(clock.instant());
        charges.save(charge);

        loan.setChargeResult("GENERADO");
        lostLoans.save(loan);
    }

    @Transactional
    public Charge changeStatus(long id, String newStatus, String reason, String actor) {
        Charge charge = charges.findById(id)
            .orElseThrow(() -> DomainException.notFound("CHARGE_NOT_FOUND", "No existe ese cobro."));

        if (!List.of("PAGADO", "ANULADO").contains(newStatus)) {
            throw DomainException.badRequest("INVALID_CHARGE_STATUS", "Estado de cobro inválido.");
        }

        if (newStatus.equals("ANULADO") && (reason == null || reason.isBlank())) {
            throw DomainException.badRequest("REASON_REQUIRED", "El motivo de anulación es obligatorio.");
        }

        if (!charge.getStatus().equals("PENDIENTE")) {
            throw DomainException.conflict("CHARGE_CLOSED", "El cobro ya fue cerrado.");
        }

        String oldStatus = charge.getStatus();
        charge.setStatus(newStatus);
        charges.save(charge);

        ChargeAudit audit = new ChargeAudit();
        audit.setChargeId(id);
        audit.setChangedBy(actor);
        audit.setPreviousStatus(oldStatus);
        audit.setNewStatus(newStatus);
        audit.setReason(reason);
        audit.setChangedAt(clock.instant());
        audits.save(audit);

        return charge;
    }

    @Transactional
    public CostCatalog upsertCost(String type, BigDecimal amount, boolean active) {
        if (type == null || type.isBlank() || amount == null || amount.signum() < 0) {
            throw DomainException.badRequest("INVALID_COST", "Tipo y costo cero o mayor son obligatorios.");
        }

        if (active) {
            costs.findAllByOrderByResourceTypeAscIdDesc().stream()
                .filter(c -> c.isActive() && c.getResourceType().equalsIgnoreCase(type))
                .forEach(c -> c.setActive(false));
        }

        CostCatalog row = new CostCatalog();
        row.setResourceType(type.trim());
        row.setAmount(amount);
        row.setActive(active);

        return costs.save(row);
    }
}
