package com.restaurant.app.core.common;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.config.api.BranchConfigService;
import com.restaurant.app.core.config.api.TenantConfigService;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantConfigService tenantConfigService;
    private final BranchConfigService branchConfigService;

    public DataInitializer(
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        TenantConfigService tenantConfigService,
        BranchConfigService branchConfigService
    ) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantConfigService = tenantConfigService;
        this.branchConfigService = branchConfigService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.findByNitOrTaxId("12345678").isPresent()) {
            log.info("Demo tenant already initialized.");
            return;
        }

        log.info("Initializing demo tenant, branch, users and dynamic configuration...");

        // 1. Demo Tenant
        Tenant tenant = new Tenant("Restaurante Demo", "12345678");
        tenant = tenantRepository.save(tenant);

        // 2. Demo Branch
        Branch branch = new Branch(tenant.getId(), "Sucursal Central", "America/La_Paz");
        branch = branchRepository.save(branch);

        // 3. Demo Admin User
        AppUser admin = new AppUser(
            tenant.getId(),
            branch.getId(),
            "admin",
            "admin@restaurant.com",
            passwordEncoder.encode("admin123"),
            "ADMIN_TENANT"
        );
        admin.setEmailVerified(true);
        appUserRepository.save(admin);

        // 4. Demo Cashier User (without email per spec)
        AppUser cashier = new AppUser(
            tenant.getId(),
            branch.getId(),
            "cajero1",
            null,
            passwordEncoder.encode("cajero123"),
            "CASHIER"
        );
        cashier.setEmailVerified(false);
        appUserRepository.save(cashier);

        // 5. Default Settings
        tenantConfigService.setSetting(
            tenant.getId(),
            "INVENTORY_DEDUCTION_MODE",
            "AUTOMATIC_PERMISSIVE",
            "STRING",
            "Modo de deducción de recetas en Kardex"
        );
        tenantConfigService.setSetting(
            tenant.getId(),
            "REQUIRE_USER_EMAIL",
            "false",
            "BOOLEAN",
            "Exigir correo electrónico obligatorio para cajeros"
        );
        branchConfigService.setSetting(
            tenant.getId(),
            branch.getId(),
            "ALLOW_OFFLINE_ORDERS",
            "true",
            "BOOLEAN",
            "Permitir registro de órdenes offline"
        );

        log.info("Demo data successfully initialized (tenant: {}, branch: {}, admin: admin, cashier: cajero1).",
            tenant.getId(), branch.getId());
    }
}
