package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import com.restaurant.app.modules.company.api.BranchDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
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

    private final CompanyPublicApi companyPublicApi;
    private final BranchPublicApi branchPublicApi;
    private final UserPublicApi userPublicApi;
    private final CashShiftRepository cashShiftRepository;
    private final ExpenseRepository expenseRepository;

    FinanceDataInitializer(
        CompanyPublicApi companyPublicApi,
        BranchPublicApi branchPublicApi,
        UserPublicApi userPublicApi,
        CashShiftRepository cashShiftRepository,
        ExpenseRepository expenseRepository
    ) {
        this.companyPublicApi = companyPublicApi;
        this.branchPublicApi = branchPublicApi;
        this.userPublicApi = userPublicApi;
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
        Optional<TenantDto> demoTenantOpt = companyPublicApi.findCompanyByNitOrTaxId(nit);
        if (demoTenantOpt.isEmpty()) {
            return;
        }

        TenantDto tenant = demoTenantOpt.get();
        List<BranchDto> branches = branchPublicApi.getBranchesByTenant(tenant.id());
        if (branches.isEmpty()) {
            return;
        }

        Optional<UserSummaryDto> adminOpt = userPublicApi.findByTenantIdAndUsername(tenant.id(), adminUsername);
        if (adminOpt.isEmpty()) {
            return;
        }
        UserSummaryDto admin = adminOpt.get();

        for (BranchDto branch : branches) {
            // 1. Verificar si ya existe un turno abierto para esta sucursal
            Optional<CashShift> existingShift = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(
                tenant.id(), branch.id(), "OPEN"
            );

            CashShift shift;
            if (existingShift.isEmpty()) {
                log.info("Initializing demo open cash shift for branch '{}' (tenant '{}')...", branch.name(), tenant.legalName());
                shift = new CashShift(tenant.id(), branch.id(), admin.id(), initialCashFloat);
                shift = cashShiftRepository.save(shift);
            } else {
                shift = existingShift.get();
            }

            // 2. Semillas de gastos para demostración si no existen
            List<Expense> existingExpenses = expenseRepository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(
                tenant.id(), branch.id()
            );

            if (existingExpenses.isEmpty()) {
                log.info("Initializing demo expenses for branch '{}' (tenant '{}')...", branch.name(), tenant.legalName());

                // Gasto Aprobado
                Expense approvedExpense = new Expense(
                    tenant.id(),
                    branch.id(),
                    shift.getId(),
                    "DAILY_OPERATIONAL",
                    approvedExpenseDesc,
                    approvedExpenseAmount,
                    true,
                    "APPROVED",
                    "https://images.unsplash.com/photo-1584473457406-6240486418e9?w=400",
                    LocalDate.now(),
                    admin.id()
                );
                approvedExpense.setApprovedBy(admin.id());
                approvedExpense.setApprovalNotes("Gasto operativo autorizado para atención de salón.");
                expenseRepository.save(approvedExpense);

                // Gasto Pendiente de Aprobación
                Optional<UserSummaryDto> cashierOpt = userPublicApi.findByTenantIdAndUsername(tenant.id(), cashierUsername);
                UserSummaryDto cashier = cashierOpt.orElse(admin);

                Expense pendingExpense = new Expense(
                    tenant.id(),
                    branch.id(),
                    shift.getId(),
                    "DAILY_OPERATIONAL",
                    pendingExpenseDesc,
                    pendingExpenseAmount,
                    true,
                    "PENDING_APPROVAL",
                    "https://images.unsplash.com/photo-1554415707-9e4c019acb45?w=400",
                    LocalDate.now(),
                    cashier.id()
                );
                expenseRepository.save(pendingExpense);
            }
        }
    }
}
