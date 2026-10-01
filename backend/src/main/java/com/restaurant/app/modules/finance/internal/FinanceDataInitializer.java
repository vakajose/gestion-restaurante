package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Order(30)
class FinanceDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FinanceDataInitializer.class);

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository appUserRepository;
    private final CashShiftRepository cashShiftRepository;
    private final ExpenseRepository expenseRepository;

    FinanceDataInitializer(
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        AppUserRepository appUserRepository,
        CashShiftRepository cashShiftRepository,
        ExpenseRepository expenseRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.appUserRepository = appUserRepository;
        this.cashShiftRepository = cashShiftRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Optional<Tenant> demoTenantOpt = tenantRepository.findByNitOrTaxId("12345678");
        if (demoTenantOpt.isEmpty()) {
            return;
        }

        Tenant tenant = demoTenantOpt.get();
        List<Branch> branches = branchRepository.findByTenantId(tenant.getId());
        if (branches.isEmpty()) {
            return;
        }

        Branch branch = branches.get(0);
        Optional<AppUser> adminOpt = appUserRepository.findByTenantIdAndUsername(tenant.getId(), "admin");
        if (adminOpt.isEmpty()) {
            return;
        }
        AppUser admin = adminOpt.get();

        // 1. Verificar si ya existe un turno abierto para Sucursal Central
        Optional<CashShift> existingShift = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(
            tenant.getId(), branch.getId(), "OPEN"
        );

        CashShift shift;
        if (existingShift.isEmpty()) {
            log.info("Initializing demo open cash shift for branch '{}'...", branch.getName());
            shift = new CashShift(tenant.getId(), branch.getId(), admin.getId(), new BigDecimal("200.00"));
            shift = cashShiftRepository.save(shift);
        } else {
            shift = existingShift.get();
        }

        // 2. Semillas de gastos para demostración si no existen
        List<Expense> existingExpenses = expenseRepository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(
            tenant.getId(), branch.getId()
        );

        if (existingExpenses.isEmpty()) {
            log.info("Initializing demo expenses for branch '{}'...", branch.getName());

            // Gasto Aprobado (materiales de limpieza)
            Expense approvedExpense = new Expense(
                tenant.getId(),
                branch.getId(),
                shift.getId(),
                "DAILY_OPERATIONAL",
                "Compra de servilletas, bolsas y detergente",
                new BigDecimal("25.00"),
                true,
                "APPROVED",
                "https://images.unsplash.com/photo-1584473457406-6240486418e9?w=400",
                LocalDate.now(),
                admin.getId()
            );
            approvedExpense.setApprovedBy(admin.getId());
            approvedExpense.setApprovalNotes("Gasto operativo autorizado para atención de salón.");
            expenseRepository.save(approvedExpense);

            // Gasto Pendiente de Aprobación (caja chica registrado por cajero con foto)
            Optional<AppUser> cashierOpt = appUserRepository.findByTenantIdAndUsername(tenant.getId(), "cajero1");
            AppUser cashier = cashierOpt.orElse(admin);

            Expense pendingExpense = new Expense(
                tenant.getId(),
                branch.getId(),
                shift.getId(),
                "DAILY_OPERATIONAL",
                "Hielo extra de emergencia para bar",
                new BigDecimal("15.00"),
                true,
                "PENDING_APPROVAL",
                "https://images.unsplash.com/photo-1554415707-9e4c019acb45?w=400",
                LocalDate.now(),
                cashier.getId()
            );
            expenseRepository.save(pendingExpense);
        }

        log.info("Finance demo data initialized successfully.");
    }
}
