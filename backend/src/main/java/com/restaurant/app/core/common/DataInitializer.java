package com.restaurant.app.core.common;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.config.api.BranchConfigService;
import com.restaurant.app.core.config.api.TenantConfigService;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.Role;
import com.restaurant.app.core.security.RolePermission;
import com.restaurant.app.core.security.RolePermissionRepository;
import com.restaurant.app.core.security.RoleRepository;
import com.restaurant.app.core.security.SystemPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantConfigService tenantConfigService;
    private final BranchConfigService branchConfigService;

    public DataInitializer(
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        AppUserRepository appUserRepository,
        RoleRepository roleRepository,
        RolePermissionRepository rolePermissionRepository,
        PasswordEncoder passwordEncoder,
        TenantConfigService tenantConfigService,
        BranchConfigService branchConfigService
    ) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
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

        log.info("Initializing demo tenant, branch, users, roles, permissions and dynamic configuration...");

        // 1. Demo Tenant
        Tenant tenant = new Tenant("Restaurante Demo", "12345678", "demo-restaurant");
        tenant = tenantRepository.save(tenant);

        // 2. Demo Roles & Granular Permissions
        seedRolesAndPermissions(tenant.getId());

        // 3. Demo Branches (Central y Sur para demostrar multi-sucursal)
        Branch branch = new Branch(tenant.getId(), "Sucursal Central", "America/La_Paz");
        branch = branchRepository.save(branch);

        Branch branch2 = new Branch(tenant.getId(), "Sucursal Sur", "America/La_Paz");
        branchRepository.save(branch2);

        // 4. Demo Admin User
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

        // 5. Demo Cashier User (without email per spec)
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

        // 6. Default Settings
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
            "Permite crear cajeros y cocineros sin correo electrónico"
        );
        tenantConfigService.setSetting(
            tenant.getId(),
            "ALLOW_DYNAMIC_REGISTRATION",
            "true",
            "BOOLEAN",
            "Permite registro público de nuevos restaurantes"
        );

        // Branch-level Settings
        branchConfigService.setSetting(
            tenant.getId(),
            branch.getId(),
            "AUTO_PRINT_TICKETS",
            "true",
            "BOOLEAN",
            "Impresión automática de tickets de comanda"
        );
        branchConfigService.setSetting(
            tenant.getId(),
            branch.getId(),
            "MAX_CASH_EXPENSE_LIMIT",
            "200.00",
            "NUMBER",
            "Límite máximo de gasto menor en efectivo sin autorización"
        );

        log.info("Demo data initialized successfully with tenant_id={}, branch_id={}", tenant.getId(), branch.getId());
    }

    private void seedRolesAndPermissions(UUID tenantId) {
        Role adminRole = roleRepository.save(new Role(tenantId, "ADMIN_TENANT", "Administrador de Empresa", "Acceso total a la administración de la empresa", true));
        Role managerRole = roleRepository.save(new Role(tenantId, "BRANCH_MANAGER", "Gerente de Sucursal", "Gestión operativa, inventarios y caja de sucursal", true));
        Role cashierRole = roleRepository.save(new Role(tenantId, "CASHIER", "Cajero", "Operación de ventas y punto de venta", true));
        Role kitchenRole = roleRepository.save(new Role(tenantId, "KITCHEN", "Cocina", "Visualización de comandas y recetas", true));

        savePermissions(tenantId, adminRole.getId(), SystemPermissions.getDefaultPermissionsForRole("ADMIN_TENANT"));
        savePermissions(tenantId, managerRole.getId(), SystemPermissions.getDefaultPermissionsForRole("BRANCH_MANAGER"));
        savePermissions(tenantId, cashierRole.getId(), SystemPermissions.getDefaultPermissionsForRole("CASHIER"));
        savePermissions(tenantId, kitchenRole.getId(), SystemPermissions.getDefaultPermissionsForRole("KITCHEN"));
    }

    private void savePermissions(UUID tenantId, UUID roleId, Set<String> permissions) {
        for (String perm : permissions) {
            rolePermissionRepository.save(new RolePermission(tenantId, roleId, perm));
        }
    }
}
