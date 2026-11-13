package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.security.JwtService;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class BackofficeSaaSIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformAdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformJwtService platformJwtService;

    @Autowired
    private JwtService tenantJwtService;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    private MockMvc mockMvc;
    private PlatformAdmin superAdmin;
    private String superAdminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        superAdmin = adminRepository.save(new PlatformAdmin(
            "bo_sa_" + suffix,
            "bo_sa_" + suffix + "@platform.local",
            "Backoffice SA Test",
            passwordEncoder.encode("ClaveSAPlatform2026!"),
            false,
            PlatformRole.SUPERADMIN
        ));
        superAdminToken = "Bearer " + platformJwtService.generateToken(superAdmin);
    }

    @Test
    @DisplayName("RBAC Plataforma y ABM Admins: PLATFORM_OPERATOR opera soporte/cobros y queda bloqueado en endpoints SUPERADMIN")
    void platformRoleIsolationAndAdminLifecycle() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String operatorUsername = "op_" + suffix;

        // 1. Token tenant ERP rechazado (401)
        String tenantToken = "Bearer " + tenantJwtService.generateToken(
            UUID.randomUUID(), "admin", "ADMIN_TENANT", UUID.randomUUID(), UUID.randomUUID()
        );
        mockMvc.perform(get("/api/v1/backoffice/dashboard/overview")
                .header("Authorization", tenantToken))
            .andExpect(status().isUnauthorized());

        // 2. SUPERADMIN crea cuenta PLATFORM_OPERATOR
        MvcResult createOpRes = mockMvc.perform(post("/api/v1/backoffice/admins")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "%s",
                        "email": "%s@platform.local",
                        "fullName": "Operador Soporte",
                        "initialPassword": "OperatorInitPass123!",
                        "role": "PLATFORM_OPERATOR"
                    }
                    """.formatted(operatorUsername, operatorUsername)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("PLATFORM_OPERATOR"))
            .andExpect(jsonPath("$.mustChangePassword").value(true))
            .andReturn();

        UUID operatorId = UUID.fromString(
            objectMapper.readTree(createOpRes.getResponse().getContentAsString()).get("id").asText()
        );

        // 3. Login como PLATFORM_OPERATOR y cambio de clave
        MvcResult opLoginRes = mockMvc.perform(post("/api/v1/backoffice/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login": "%s", "password": "OperatorInitPass123!"}
                    """.formatted(operatorUsername)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.admin.role").value("PLATFORM_OPERATOR"))
            .andReturn();

        String opToken = "Bearer " + objectMapper.readTree(opLoginRes.getResponse().getContentAsString())
            .get("accessToken").asText();

        mockMvc.perform(post("/api/v1/backoffice/auth/change-password")
                .header("Authorization", opToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword": "OperatorInitPass123!", "newPassword": "OperatorNewPass2026!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(false));

        // 4. PLATFORM_OPERATOR puede ver dashboard, planes y salud/métricas
        mockMvc.perform(get("/api/v1/backoffice/dashboard/overview").header("Authorization", opToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTenants").isNumber());

        mockMvc.perform(get("/api/v1/backoffice/plans").header("Authorization", opToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.code == 'WEEKLY_FLEX')]").exists());

        mockMvc.perform(get("/api/v1/backoffice/observability/health").header("Authorization", opToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        // 5. PLATFORM_OPERATOR NO puede gestionar admins, settings, loggers PUT, audit-logs ni crear planes (403)
        mockMvc.perform(get("/api/v1/backoffice/admins").header("Authorization", opToken))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", opToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"X1","name":"X","billingCycle":"MONTHLY","durationDays":30,"price":100}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/backoffice/settings/storage.max_file_size_bytes")
                .header("Authorization", opToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"value":"1048576"}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/backoffice/observability/loggers/com.restaurant.app")
                .header("Authorization", opToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"configuredLevel":"DEBUG"}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/backoffice/audit-logs").header("Authorization", opToken))
            .andExpect(status().isForbidden());

        // 6. Autodesactivación rechazada y desactivación del operador por SA exitosa
        mockMvc.perform(patch("/api/v1/backoffice/admins/" + superAdmin.getId() + "/status")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"isActive": false}
                    """))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/backoffice/admins/" + operatorId + "/status")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"isActive": false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Ciclo completo SaaS: Plan pendiente (uso adelantado), FIFO pagos, 2 bloqueos, gracia por ventas, downgrade no destructivo, telemetría y Actuator")
    void fullBackofficeSubscriptionSuspensionDowngradeTelemetryAndOpsFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "saas-e2e-" + suffix;

        // ---------------------------------------------------------------------
        // A. CRUD de Plan custom desde Backoffice
        // ---------------------------------------------------------------------
        MvcResult planRes = mockMvc.perform(post("/api/v1/backoffice/plans")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "code": "CUSTOM_%s",
                        "name": "Plan E2E Custom",
                        "description": "Plan para pruebas integrales",
                        "billingCycle": "WEEKLY",
                        "durationDays": 7,
                        "price": 120.00,
                        "currency": "BOB",
                        "maxBranches": 2,
                        "maxUsers": 3,
                        "maxDishes": 4,
                        "maxInventoryItems": 50,
                        "maxMonthlyOrders": 100
                    }
                    """.formatted(suffix.toUpperCase())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.price").value(120.00))
            .andReturn();

        UUID customPlanId = UUID.fromString(
            objectMapper.readTree(planRes.getResponse().getContentAsString()).get("id").asText()
        );

        // ---------------------------------------------------------------------
        // B. Onboarding de tenant -> auto-provisión TRIAL_14D
        // ---------------------------------------------------------------------
        MvcResult onbRes = mockMvc.perform(post("/api/v1/backoffice/tenants")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "company": {
                            "legalName": "Restaurante SaaS %s",
                            "slug": "%s",
                            "nitOrTaxId": "888%s"
                        },
                        "branch": {
                            "name": "Casa Matriz E2E",
                            "address": "Av. Ballivián 100"
                        },
                        "businessTemplate": "TRADITIONAL",
                        "includeDemoCatalog": true,
                        "adminUser": {
                            "fullName": "Ana Admin",
                            "username": "aadmin",
                            "email": "aadmin@%s.bo",
                            "password": "TenantPassword123!"
                        }
                    }
                    """.formatted(suffix, slug, suffix.substring(0, 5), suffix)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode onbJson = objectMapper.readTree(onbRes.getResponse().getContentAsString());
        UUID tenantId = UUID.fromString(onbJson.get("tenantId").asText());
        UUID branchId = UUID.fromString(onbJson.get("branchId").asText());
        DishPriceDto demoDish = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId).getFirst();

        // Verificar TRIAL_14D asignado automáticamente al tenant
        mockMvc.perform(get("/api/v1/backoffice/tenants/" + tenantId)
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entitlements.managedSubscription").value(true))
            .andExpect(jsonPath("$.entitlements.planCode").value("TRIAL_14D"))
            .andExpect(jsonPath("$.entitlements.status").value("TRIAL"));

        // Login ERP tenant admin
        MvcResult erpLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tenantIdentifier": "%s",
                        "login": "aadmin",
                        "password": "TenantPassword123!"
                    }
                    """.formatted(slug)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode erpLoginJson = objectMapper.readTree(erpLoginRes.getResponse().getContentAsString());
        String erpToken = erpLoginJson.get("token").asText();
        String erpRefreshToken = erpLoginJson.get("refreshToken").asText();

        // ---------------------------------------------------------------------
        // C. Asignar Plan en PENDIENTE DE PAGO ("uso por adelantado") + Conciliación FIFO + Condonación
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/subscription/assign-plan")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "planId": "%s",
                        "markAsPaid": false,
                        "notes": "Crédito semanal otorgado por adelantado"
                    }
                    """.formatted(customPlanId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.pendingBalance").value(120.00))
            .andExpect(jsonPath("$.posOperationsAllowed").value(true));

        // Segundo cargo pendiente via plan WEEKLY_FLEX para probar FIFO entre múltiples cargos
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/subscription/assign-plan")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "planCode": "WEEKLY_FLEX",
                        "markAsPaid": false,
                        "notes": "Renovación semanal a crédito"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingBalance").value(195.00));

        // Pago de 150 BOB debe cancelar cargo #1 (120) y abonar parcialmente cargo #2 (30/75) -> deuda 45 BOB
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/payments")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "amount": 150.00,
                        "currency": "BOB",
                        "paymentMethod": "QR",
                        "referenceCode": "QR-FIFO-001",
                        "notes": "Abono combinado FIFO"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.remainingPendingBalance").value(45.00));

        MvcResult chargesRes = mockMvc.perform(get("/api/v1/backoffice/tenants/" + tenantId + "/charges")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPendingBalance").value(45.00))
            .andReturn();

        JsonNode chargesArray = objectMapper.readTree(chargesRes.getResponse().getContentAsString()).get("charges");
        UUID partialChargeId = null;
        for (JsonNode ch : chargesArray) {
            if ("PARTIAL".equals(ch.get("chargeStatus").asText())) {
                partialChargeId = UUID.fromString(ch.get("id").asText());
            }
        }
        assertThat(partialChargeId).isNotNull();

        mockMvc.perform(patch("/api/v1/backoffice/charges/" + partialChargeId + "/waive")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason": "Condonación saldo menor"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chargeStatus").value("WAIVED"));

        mockMvc.perform(get("/api/v1/backoffice/tenants/" + tenantId + "/charges")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPendingBalance").value(0));

        // ---------------------------------------------------------------------
        // D. Abrir caja + Modos de Suspensión: POS_AND_SHIFT_ONLY vs TOTAL
        // ---------------------------------------------------------------------
        MvcResult shiftRes = mockMvc.perform(post("/api/v1/finance/shifts/open")
                .header("Authorization", "Bearer " + erpToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"branchId": "%s", "initialCash": 100.00}
                    """.formatted(branchId)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID cashShiftId = UUID.fromString(
            objectMapper.readTree(shiftRes.getResponse().getContentAsString()).get("id").asText()
        );

        // Suspensión parcial POS_AND_SHIFT_ONLY: login ERP y /company/entitlements siguen OK; POS rechazado 403
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/subscription/suspend")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"mode": "POS_AND_SHIFT_ONLY", "reason": "Mora operativa preventiva"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectiveSuspensionMode").value("POS_AND_SHIFT_ONLY"))
            .andExpect(jsonPath("$.loginAllowed").value(true))
            .andExpect(jsonPath("$.posOperationsAllowed").value(false));

        mockMvc.perform(get("/api/v1/company/entitlements")
                .header("Authorization", "Bearer " + erpToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectiveSuspensionMode").value("POS_AND_SHIFT_ONLY"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantIdentifier": "%s", "login": "aadmin", "password": "TenantPassword123!"}
                    """.formatted(slug)))
            .andExpect(status().isOk());

        String posOrderJson = """
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "cashShiftId": "%s",
                "paymentMethod": "CASH",
                "items": [{"dishId": "%s", "quantity": 1, "unitPrice": 40.00}]
            }
            """;

        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + erpToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(posOrderJson.formatted(UUID.randomUUID(), branchId, cashShiftId, demoDish.dishId())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_OPERATIONS_SUSPENDED"));

        // Suspensión TOTAL: bloquea access JWT en filtro, /auth/refresh (revocado) y /auth/login
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/subscription/suspend")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"mode": "TOTAL", "reason": "Suspensión total por fraude o impago severo"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loginAllowed").value(false));

        mockMvc.perform(get("/api/v1/company/entitlements")
                .header("Authorization", "Bearer " + erpToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tenantIdentifier": "%s", "login": "aadmin", "password": "TenantPassword123!"}
                    """.formatted(slug)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(erpRefreshToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_SUSPENDED"));

        // ---------------------------------------------------------------------
        // E. Gracia Condicional (graceMaxOrders = 1) -> 1ra venta pasa, 2da venta auto-suspendida
        // ---------------------------------------------------------------------
        Instant graceEnd = Instant.now().plus(2, ChronoUnit.DAYS);
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/subscription/grace")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "graceUntil": "%s",
                        "graceMaxOrders": 1,
                        "notes": "1 venta de gracia habilitada"
                    }
                    """.formatted(graceEnd.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("GRACE_PERIOD"))
            .andExpect(jsonPath("$.posOperationsAllowed").value(true));

        // 1ra venta en gracia -> 201 Created y dispara decremento/consumo de gracia síncrono
        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + erpToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(posOrderJson.formatted(UUID.randomUUID(), branchId, cashShiftId, demoDish.dishId())))
            .andExpect(status().isCreated());

        // 2da venta en gracia -> bloqueada inmediatamente por auto-suspensión al alcanzar graceMaxOrders=1
        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + erpToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(posOrderJson.formatted(UUID.randomUUID(), branchId, cashShiftId, demoDish.dishId())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("TENANT_OPERATIONS_SUSPENDED"));

        // ---------------------------------------------------------------------
        // F. Reactivación + Downgrade no destructivo de Usuarios + Resolución manual en ERP + Límite de Platos
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/v1/backoffice/tenants/" + tenantId + "/subscription/reactivate")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason": "Regularización", "extendDays": 30}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Crear un 2do usuario activo en el tenant
        MvcResult user2Res = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + erpToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "cajero_extra",
                        "fullName": "Cajero Extra",
                        "email": "cajero@%s.bo",
                        "password": "CajeroPass123",
                        "role": "CASHIER",
                        "branchId": "%s"
                    }
                    """.formatted(suffix, branchId)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID user2Id = UUID.fromString(
            objectMapper.readTree(user2Res.getResponse().getContentAsString()).get("id").asText()
        );

        // Downgrade de maxUsers = 1 y maxDishes = 3 (ya existen 3 platos demo)
        mockMvc.perform(put("/api/v1/backoffice/tenants/" + tenantId + "/subscription/quotas")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "maxBranches": 1,
                        "maxUsers": 1,
                        "maxDishes": 3,
                        "maxInventoryItems": 50,
                        "maxMonthlyOrders": 100
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loginAllowed").value(true))
            .andExpect(jsonPath("$.posOperationsAllowed").value(false))
            .andExpect(jsonPath("$.overQuotaResources[0]").value("USERS"));

        // No se eliminó ningún usuario; el POS queda bloqueado hasta que el admin resuelva el exceso
        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + erpToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(posOrderJson.formatted(UUID.randomUUID(), branchId, cashShiftId, demoDish.dishId())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("OVER_QUOTA_RESOLUTION_REQUIRED"));

        // Admin ERP desactiva manualmente al 2do usuario sin perder datos -> POS vuelve a funcionar
        mockMvc.perform(patch("/api/v1/users/" + user2Id + "/status?isActive=false")
                .header("Authorization", "Bearer " + erpToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + erpToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(posOrderJson.formatted(UUID.randomUUID(), branchId, cashShiftId, demoDish.dishId())))
            .andExpect(status().isCreated());

        // Intento de crear 4to plato cuando maxDishes = 3 debe responder QUOTA_EXCEEDED
        MvcResult catsRes = mockMvc.perform(get("/api/v1/catalog/categories")
                .header("Authorization", "Bearer " + erpToken))
            .andExpect(status().isOk())
            .andReturn();
        UUID categoryId = UUID.fromString(
            objectMapper.readTree(catsRes.getResponse().getContentAsString()).get(0).get("id").asText()
        );

        mockMvc.perform(post("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + erpToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "categoryId": "%s",
                        "code": "P-EXTRA-01",
                        "name": "Plato Sobre Cuota",
                        "salePrice": 30.00,
                        "isActive": true
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("QUOTA_EXCEEDED"));

        // ---------------------------------------------------------------------
        // G. Telemetría CQRS asíncrona (2 ventas pagadas), Dashboard y Cartera 360°
        // ---------------------------------------------------------------------
        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() ->
                mockMvc.perform(get("/api/v1/backoffice/tenants/" + tenantId + "/telemetry")
                        .header("Authorization", superAdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.paidOrdersCount").value(2))
                    .andExpect(jsonPath("$.summary.grossSalesAmount").value(80.00))
                    .andExpect(jsonPath("$.dailySeries.length()").value(1))
                    .andExpect(jsonPath("$.branchBreakdown[0].branchId").value(branchId.toString()))
                    .andExpect(jsonPath("$.structuralUsage.totalUsers").value(2))
                    .andExpect(jsonPath("$.structuralUsage.activeUsers").value(1))
            );

        mockMvc.perform(get("/api/v1/backoffice/tenants?search=" + slug)
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].slug").value(slug))
            .andExpect(jsonPath("$[0].salesLast30Days").value(80.00));

        // ---------------------------------------------------------------------
        // H. Storage active probe, Actuator Health/Metrics/Loggers Hot-Reload y Audit Logs
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/v1/backoffice/storage/test-connection")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("LOCAL"))
            .andExpect(jsonPath("$.reachable").value(true));

        mockMvc.perform(get("/api/v1/backoffice/observability/metrics")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAvailableMeters").isNumber());

        mockMvc.perform(put("/api/v1/backoffice/observability/loggers/com.restaurant.app.modules.pos")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"configuredLevel": "DEBUG"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectiveLevel").value("DEBUG"));

        // Restaurar logger a INFO
        mockMvc.perform(put("/api/v1/backoffice/observability/loggers/com.restaurant.app.modules.pos")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"configuredLevel": "INFO"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectiveLevel").value("INFO"));

        mockMvc.perform(get("/api/v1/backoffice/observability/environment")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeProfiles[0]").value("test"));

        mockMvc.perform(get("/api/v1/backoffice/audit-logs?action=LOGGER_LEVEL_CHANGED")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].action").value("LOGGER_LEVEL_CHANGED"));

        mockMvc.perform(get("/api/v1/backoffice/audit-logs?action=AUTO_SUSPENDED_GRACE_EXHAUSTED")
                .header("Authorization", superAdminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].targetId").value(tenantId.toString()));
    }
}
