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
        seedTenantFinance(
            "12345678", "admin", "cajero1", new BigDecimal("200.00"),
            "Compra de servilletas, bolsas y detergente", new BigDecimal("25.00"),
            "Hielo extra de emergencia para bar", new BigDecimal("15.00")
        );

        seedTenantFinance(
            "87654321", "admin_burger", "cajero_burger", new BigDecimal("300.00"),
            "Compra de carbón vegetal y bolsas térmicas para delivery", new BigDecimal("45.00"),
            "Reposición de hielo en cubos y limones para bar", new BigDecimal("20.00")
        );

        seedTenantFinance(
            "98765432", "admin_italia", "cajero_italia", new BigDecimal("250.00"),
            "Compra de albahaca fresca del mercado y orégano", new BigDecimal("30.00"),
            "Caja de guantes descartables y servilletas de salón", new BigDecimal("18.00")
        );

        log.info("Finance demo data initialized successfully for all tenants.");
    }

    private void seedTenantFinance(
        String nit,
        String adminUsername,
        String cashierUsername,
        BigDecimal initialCashFloat,
        String approvedExpenseDesc,
        BigDecimal approvedExpenseAmount,
        String pendingExpenseDesc,
        BigDecimal pendingExpenseAmount
    ) {
        Optional<Tenant> demoTenantOpt = tenantRepository.findByNitOrTaxId(nit);
        if (demoTenantOpt.isEmpty()) {
            return;
        }

        Tenant tenant = demoTenantOpt.get();
        List<Branch> branches = branchRepository.findByTenantId(tenant.getId());
        if (branches.isEmpty()) {
            return;
        }

        Branch branch = branches.get(0);
        Optional<AppUser> adminOpt = appUserRepository.findByTenantIdAndUsername(tenant.getId(), adminUsername);
        if (adminOpt.isEmpty()) {
            return;
        }
        AppUser admin = adminOpt.get();

        // 1. Verificar si ya existe un turno abierto para esta sucursal
        Optional<CashShift> existingShift = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(
            tenant.getId(), branch.getId(), "OPEN"
        );

        CashShift shift;
        if (existingShift.isEmpty()) {
            log.info("Initializing demo open cash shift for branch '{}' (tenant '{}')...", branch.getName(), tenant.getLegalName());
            shift = new CashShift(tenant.getId(), branch.getId(), admin.getId(), initialCashFloat);
            shift = cashShiftRepository.save(shift);
        } else {
            shift = existingShift.get();
        }

        // 2. Semillas de gastos para demostración si no existen
        List<Expense> existingExpenses = expenseRepository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(
            tenant.getId(), branch.getId()
        );

        if (existingExpenses.isEmpty()) {
            log.info("Initializing demo expenses for branch '{}' (tenant '{}')...", branch.getName(), tenant.getLegalName());

            // Gasto Aprobado
            Expense approvedExpense = new Expense(
                tenant.getId(),
                branch.getId(),
                shift.getId(),
                "DAILY_OPERATIONAL",
                approvedExpenseDesc,
                approvedExpenseAmount,
                true,
                "APPROVED",
                "https://images.unsplash.com/photo-1584473457406-6240486418e9?w=400",
                LocalDate.now(),
                admin.getId()
            );
            approvedExpense.setApprovedBy(admin.getId());
            approvedExpense.setApprovalNotes("Gasto operativo autorizado para atención de salón.");
            expenseRepository.save(approvedExpense);

            // Gasto Pendiente de Aprobación
            Optional<AppUser> cashierOpt = appUserRepository.findByTenantIdAndUsername(tenant.getId(), cashierUsername);
            AppUser cashier = cashierOpt.orElse(admin);

            Expense pendingExpense = new Expense(
                tenant.getId(),
                branch.getId(),
                shift.getId(),
                "DAILY_OPERATIONAL",
                pendingExpenseDesc,
                pendingExpenseAmount,
                true,
                "PENDING_APPROVAL",
                "https://images.unsplash.com/photo-1554415707-9e4c019acb45?w=400",
                LocalDate.now(),
                cashier.getId()
            );
            expenseRepository.save(pendingExpense);
        }
    }
}
