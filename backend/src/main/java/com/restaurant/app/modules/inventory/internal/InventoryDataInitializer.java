package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Order(20)
class InventoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitializer.class);

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final BranchStockRepository branchStockRepository;
    private final KardexService kardexService;

    InventoryDataInitializer(
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        CatalogPublicApi catalogPublicApi,
        BranchStockRepository branchStockRepository,
        KardexService kardexService
    ) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.catalogPublicApi = catalogPublicApi;
        this.branchStockRepository = branchStockRepository;
        this.kardexService = kardexService;
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

        List<BranchStock> existingStock = branchStockRepository.findByTenantIdAndBranchId(tenant.getId(), branch.getId());
        if (!existingStock.isEmpty()) {
            log.info("Demo inventory already seeded for branch {}", branch.getId());
            return;
        }

        log.info("Seeding demo inventory and initial Kardex for branch {}...", branch.getId());
        List<IngredientDto> ingredients = catalogPublicApi.getAllIngredients(tenant.getId());

        for (IngredientDto ing : ingredients) {
            BigDecimal qty;
            BigDecimal cost;

            if (ing.name().toLowerCase().contains("carne")) {
                qty = new BigDecimal("10.0000");
                cost = new BigDecimal("28.00");
            } else if (ing.name().toLowerCase().contains("pan")) {
                qty = new BigDecimal("50.0000");
                cost = new BigDecimal("1.50");
            } else if (ing.name().toLowerCase().contains("queso")) {
                qty = new BigDecimal("5.0000");
                cost = new BigDecimal("45.00");
            } else if (ing.name().toLowerCase().contains("papas")) {
                qty = new BigDecimal("20.0000");
                cost = new BigDecimal("6.00");
            } else {
                qty = new BigDecimal("30.0000");
                cost = new BigDecimal("4.50");
            }

            kardexService.registerInitialStock(
                tenant.getId(),
                branch.getId(),
                new InitialStockRequest(ing.id(), qty, cost)
            );
        }

        log.info("Initial demo stock successfully seeded for {} ingredients.", ingredients.size());
    }
}
