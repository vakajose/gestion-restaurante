package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.company.api.BranchDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Profile({"dev", "test"})
@Order(20)
class InventoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitializer.class);

    private final CompanyPublicApi companyPublicApi;
    private final BranchPublicApi branchPublicApi;
    private final CatalogPublicApi catalogPublicApi;
    private final BranchStockRepository branchStockRepository;
    private final KardexService kardexService;

    InventoryDataInitializer(
        CompanyPublicApi companyPublicApi,
        BranchPublicApi branchPublicApi,
        CatalogPublicApi catalogPublicApi,
        BranchStockRepository branchStockRepository,
        KardexService kardexService
    ) {
        this.companyPublicApi = companyPublicApi;
        this.branchPublicApi = branchPublicApi;
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
        Optional<TenantDto> tenantOpt = companyPublicApi.findCompanyByNitOrTaxId(nit);
        if (tenantOpt.isEmpty()) {
            return;
        }

        TenantDto tenant = tenantOpt.get();
        List<BranchDto> branches = branchPublicApi.getBranchesByTenant(tenant.id());
        if (branches.isEmpty()) {
            return;
        }

        List<IngredientDto> ingredients = catalogPublicApi.getAllIngredients(tenant.id());
        if (ingredients.isEmpty()) {
            return;
        }

        for (BranchDto branch : branches) {
            List<BranchStock> existingStock = branchStockRepository.findByTenantIdAndBranchId(tenant.id(), branch.id());
            if (!existingStock.isEmpty()) {
                log.info("Demo inventory already seeded for branch {} of tenant {}", branch.id(), tenant.legalName());
                continue;
            }

            log.info("Seeding demo inventory and initial Kardex for branch '{}' of tenant '{}'...", branch.name(), tenant.legalName());

            for (IngredientDto ing : ingredients) {
                BigDecimal qty;
                BigDecimal cost;

                String name = ing.name().toLowerCase();
                if (name.contains("carne") || name.contains("medallón")) {
                    qty = new BigDecimal("100.0000");
                    cost = new BigDecimal("25.00");
                } else if (name.contains("pan") || name.contains("masa")) {
                    qty = new BigDecimal("150.0000");
                    cost = new BigDecimal("2.50");
                } else if (name.contains("queso") || name.contains("mozzarella") || name.contains("cheddar") || name.contains("gouda") || name.contains("gorgonzola")) {
                    qty = new BigDecimal("50.0000");
                    cost = new BigDecimal("45.00");
                } else if (name.contains("tocino") || name.contains("pepperoni") || name.contains("prosciutto")) {
                    qty = new BigDecimal("50.0000");
                    cost = new BigDecimal("55.00");
                } else if (name.contains("papas") || name.contains("aros")) {
                    qty = new BigDecimal("100.0000");
                    cost = new BigDecimal("8.00");
                } else if (name.contains("coca") || name.contains("cerveza") || name.contains("vino") || name.contains("agua") || name.contains("shake")) {
                    qty = new BigDecimal("100.0000");
                    cost = new BigDecimal("6.00");
                } else {
                    qty = new BigDecimal("50.0000");
                    cost = new BigDecimal("12.00");
                }

                kardexService.registerInitialStock(
                    tenant.id(),
                    branch.id(),
                    new InitialStockRequest(ing.id(), qty, cost)
                );
            }

            log.info("Initial demo stock successfully seeded for {} ingredients in branch '{}'.", ingredients.size(), branch.name());
        }
    }
}
