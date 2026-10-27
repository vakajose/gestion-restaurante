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
        for (String nit : List.of("12345678", "87654321", "98765432")) {
            seedTenantInventory(nit);
        }
    }

    private void seedTenantInventory(String nit) {
        Optional<Tenant> tenantOpt = tenantRepository.findByNitOrTaxId(nit);
        if (tenantOpt.isEmpty()) {
            return;
        }

        Tenant tenant = tenantOpt.get();
        List<Branch> branches = branchRepository.findByTenantId(tenant.getId());
        if (branches.isEmpty()) {
            return;
        }

        Branch branch = branches.get(0);

        List<BranchStock> existingStock = branchStockRepository.findByTenantIdAndBranchId(tenant.getId(), branch.getId());
        if (!existingStock.isEmpty()) {
            log.info("Demo inventory already seeded for branch {} of tenant {}", branch.getId(), tenant.getLegalName());
            return;
        }

        log.info("Seeding demo inventory and initial Kardex for branch '{}' of tenant '{}'...", branch.getName(), tenant.getLegalName());
        List<IngredientDto> ingredients = catalogPublicApi.getAllIngredients(tenant.getId());

        for (IngredientDto ing : ingredients) {
            BigDecimal qty;
            BigDecimal cost;

            String name = ing.name().toLowerCase();
            if (name.contains("carne") || name.contains("medallón")) {
                qty = new BigDecimal("40.0000");
                cost = new BigDecimal("25.00");
            } else if (name.contains("pan") || name.contains("masa")) {
                qty = new BigDecimal("60.0000");
                cost = new BigDecimal("2.50");
            } else if (name.contains("queso") || name.contains("mozzarella") || name.contains("cheddar") || name.contains("gouda") || name.contains("gorgonzola")) {
                qty = new BigDecimal("20.0000");
                cost = new BigDecimal("45.00");
            } else if (name.contains("tocino") || name.contains("pepperoni") || name.contains("prosciutto")) {
                qty = new BigDecimal("15.0000");
                cost = new BigDecimal("55.00");
            } else if (name.contains("papas") || name.contains("aros")) {
                qty = new BigDecimal("35.0000");
                cost = new BigDecimal("8.00");
            } else if (name.contains("coca") || name.contains("cerveza") || name.contains("vino") || name.contains("agua") || name.contains("shake")) {
                qty = new BigDecimal("48.0000");
                cost = new BigDecimal("6.00");
            } else {
                qty = new BigDecimal("30.0000");
                cost = new BigDecimal("12.00");
            }

            kardexService.registerInitialStock(
                tenant.getId(),
                branch.getId(),
                new InitialStockRequest(ing.id(), qty, cost)
            );
        }

        log.info("Initial demo stock successfully seeded for {} ingredients in branch '{}'.", ingredients.size(), branch.getName());
    }
}
