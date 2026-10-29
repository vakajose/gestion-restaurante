package com.restaurant.app.modules.company;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.JwtService;
import com.restaurant.app.core.security.RoleRepository;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OnboardingIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ConfigurationPublicApi configurationPublicApi;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/onboarding aprovisiona atómicamente Tenant, Sucursal Matriz, Admin, Presets FAST_FOOD, 3 platos demo y emite JWT")
    void onboardingFastFoodWithDemoCatalogSuccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "pollos-copacabana-" + suffix;
        String nit = "4567890" + suffix.substring(0, 3);

        String payload = """
            {
                "company": {
                    "legalName": "Pollos Copacabana Express %s",
                    "slug": "%s",
                    "nitOrTaxId": "%s",
                    "logoUrl": "https://cdn.restaurant.bo/logos/pollos.png"
                },
                "branch": {
                    "name": "Sucursal El Prado",
                    "address": "Av. 16 de Julio #1450",
                    "latitude": -16.5001234,
                    "longitude": -68.1345678,
                    "phone": "+591 2 2441122",
                    "currency": "BOB"
                },
                "businessTemplate": "FAST_FOOD",
                "includeDemoCatalog": true,
                "adminUser": {
                    "fullName": "Carlos Mendoza",
                    "username": "cmendoza",
                    "email": "cmendoza@pollos.bo",
                    "password": "PasswordSegura123"
                }
            }
            """.formatted(suffix, slug, nit);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").isNotEmpty())
            .andExpect(jsonPath("$.branchId").isNotEmpty())
            .andExpect(jsonPath("$.adminUserId").isNotEmpty())
            .andExpect(jsonPath("$.slug").value(slug))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("cmendoza"))
            .andExpect(jsonPath("$.user.fullName").value("Carlos Mendoza"))
            .andExpect(jsonPath("$.user.role").value("ADMIN_TENANT"))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID tenantId = UUID.fromString(body.get("tenantId").asText());
        UUID branchId = UUID.fromString(body.get("branchId").asText());
        UUID adminUserId = UUID.fromString(body.get("adminUserId").asText());
        String accessToken = body.get("accessToken").asText();

        // 1. Verificar Tenant
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        assertThat(tenant.getSlug()).isEqualTo(slug);
        assertThat(tenant.getNitOrTaxId()).isEqualTo(nit);
        assertThat(tenant.getLogoUrl()).isEqualTo("https://cdn.restaurant.bo/logos/pollos.png");

        // 2. Verificar Branch Matriz con timezone por defecto 'America/La_Paz' y coordenadas
        Branch branch = branchRepository.findById(branchId).orElseThrow();
        assertThat(branch.getTenantId()).isEqualTo(tenantId);
        assertThat(branch.getTimezone()).isEqualTo("America/La_Paz");
        assertThat(branch.getCurrency()).isEqualTo("BOB");
        assertThat(branch.getLatitude()).isNotNull();
        assertThat(branch.getLongitude()).isNotNull();

        // 3. Verificar AdminUser con Argon2id y Roles base semillados
        AppUser admin = appUserRepository.findById(adminUserId).orElseThrow();
        assertThat(admin.getFullName()).isEqualTo("Carlos Mendoza");
        assertThat(admin.getPasswordHash()).startsWith("$argon2");
        assertThat(roleRepository.findByTenantId(tenantId)).hasSize(4);

        // 4. Verificar Presets Operativos en ConfigurationPublicApi
        assertThat(configurationPublicApi.getString(SystemParameter.INVENTORY_DEDUCTION_MODE, branchId, tenantId))
            .isEqualTo("AUTOMATIC_PERMISSIVE");
        assertThat(configurationPublicApi.getString(SystemParameter.POS_DEFAULT_ORDER_MODE, branchId, tenantId))
            .isEqualTo("TAKEOUT");
        assertThat(configurationPublicApi.getBoolean(SystemParameter.POS_DISPLAY_DISH_IMAGES, branchId, tenantId))
            .isTrue();
        assertThat(configurationPublicApi.getInt(SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES, branchId, tenantId))
            .isEqualTo(120);
        assertThat(configurationPublicApi.getString(SystemParameter.COMPANY_CURRENCY, branchId, tenantId))
            .isEqualTo("BOB");
        assertThat(configurationPublicApi.getString(SystemParameter.COMPANY_TIMEZONE, branchId, tenantId))
            .isEqualTo("America/La_Paz");

        // 5. Verificar 3 platos demo en el catálogo con recetas
        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        assertThat(menu).hasSize(3);
        for (DishPriceDto dish : menu) {
            Optional<DishRecipeDto> recipe = catalogPublicApi.getRecipeForDish(tenantId, dish.dishId());
            assertThat(recipe).isPresent();
            assertThat(recipe.get().items()).isNotEmpty();
        }

        // 6. Verificar que el JWT emitido es válido y permite consultar /api/v1/company directamente ("Ready to Sell")
        assertThat(jwtService.validateToken(accessToken)).isTrue();
        mockMvc.perform(get("/api/v1/company")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tenantId.toString()))
            .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    @DisplayName("POST /api/v1/auth/onboarding sin NIT (null o vacío) funciona para pequeños negocios informales y permite login por slug")
    void onboardingWithoutNitAndSubsequentLoginSuccess() throws Exception {
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        String slug1 = "pizzeria-napoli-" + suffix1;

        String payload1 = """
            {
                "company": {
                    "legalName": "Pizzería Napoli %s",
                    "slug": "%s",
                    "nitOrTaxId": ""
                },
                "branch": {
                    "name": "Sucursal Sopocachi",
                    "address": "Plaza Avaroa #100"
                },
                "businessTemplate": "EMPTY",
                "includeDemoCatalog": false,
                "adminUser": {
                    "fullName": "Luigi Rossi",
                    "username": "lrossi",
                    "password": "NapoliPassword123"
                }
            }
            """.formatted(suffix1, slug1);

        MvcResult result1 = mockMvc.perform(post("/api/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload1))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value(slug1))
            .andReturn();

        UUID tenantId1 = UUID.fromString(objectMapper.readTree(result1.getResponse().getContentAsString()).get("tenantId").asText());
        Tenant saved1 = tenantRepository.findById(tenantId1).orElseThrow();
        assertThat(saved1.getNitOrTaxId()).isNull();

        // Segundo negocio informal sin NIT tampoco debe colisionar en el índice único parcial
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        String slug2 = "cafeteria-aroma-" + suffix2;
        String payload2 = """
            {
                "company": {
                    "legalName": "Cafetería Aroma %s",
                    "slug": "%s",
                    "nitOrTaxId": null
                },
                "branch": {
                    "name": "Sucursal Obrajes"
                },
                "businessTemplate": "CAFE",
                "includeDemoCatalog": true,
                "adminUser": {
                    "fullName": "Ana Gomez",
                    "username": "agomez",
                    "password": "AromaPassword123"
                }
            }
            """.formatted(suffix2, slug2);

        mockMvc.perform(post("/api/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload2))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value(slug2));

        // Verificar Login posterior usando el slug como tenantIdentifier
        String loginJson = """
            {
                "tenantIdentifier": "%s",
                "login": "lrossi",
                "password": "NapoliPassword123"
            }
            """.formatted(slug1);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.tenantSlug").value(slug1))
            .andExpect(jsonPath("$.user.fullName").value("Luigi Rossi"));
    }

    @Test
    @DisplayName("Rollback atómico @Transactional: fallo intermedio o conflicto revierte Tenant y Branch sin dejar datos huérfanos")
    void atomicRollbackOnFailureLeavesNoOrphanRecords() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String orphanSlug = "empresa-rollback-" + suffix;

        // Provocamos un fallo después de persistir Tenant enviando una zona horaria inválida en Branch
        String invalidBranchTzPayload = """
            {
                "company": {
                    "legalName": "Empresa Rollback %s",
                    "slug": "%s",
                    "nitOrTaxId": "999%s"
                },
                "branch": {
                    "name": "Sucursal Fallida",
                    "timezone": "Timezone/Inexistente_Error"
                },
                "businessTemplate": "TRADITIONAL",
                "includeDemoCatalog": true,
                "adminUser": {
                    "fullName": "Admin Fallido",
                    "username": "admin_fail",
                    "password": "Password123"
                }
            }
            """.formatted(suffix, orphanSlug, suffix.substring(0, 5));

        mockMvc.perform(post("/api/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBranchTzPayload))
            .andExpect(status().isBadRequest());

        // Verificar que el Tenant NO quedó persistido en base de datos gracias a @Transactional
        assertThat(tenantRepository.findBySlug(orphanSlug)).isEmpty();

        // Verificar también rechazo por NIT duplicado (409 Conflict)
        String duplicateNitPayload = """
            {
                "company": {
                    "legalName": "Empresa NIT Duplicado",
                    "slug": "slug-unico-%s",
                    "nitOrTaxId": "12345678"
                },
                "branch": {
                    "name": "Sucursal Matriz"
                },
                "businessTemplate": "FAST_FOOD",
                "includeDemoCatalog": false,
                "adminUser": {
                    "fullName": "Admin Dup",
                    "username": "admindup",
                    "password": "Password123"
                }
            }
            """.formatted(suffix);

        mockMvc.perform(post("/api/v1/auth/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateNitPayload))
            .andExpect(status().isConflict());

        assertThat(tenantRepository.findBySlug("slug-unico-" + suffix)).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/auth/check-slug y check-availability verifican disponibilidad y sugieren slug alternativo con sufijo")
    void checkSlugAndAvailabilityEndpoints() throws Exception {
        // 1. Slug libre
        String freeSlug = "restaurante-libre-" + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(get("/api/v1/auth/check-slug")
                .param("slug", freeSlug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.suggestedAlternative").value(freeSlug))
            .andExpect(jsonPath("$.suggestedSlug").value(freeSlug));

        // 2. Slug ocupado ("demo-restaurant" semillado por DataInitializer)
        MvcResult occupiedResult = mockMvc.perform(get("/api/v1/auth/check-slug")
                .param("slug", "demo-restaurant"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false))
            .andReturn();

        JsonNode occupiedNode = objectMapper.readTree(occupiedResult.getResponse().getContentAsString());
        String suggested = occupiedNode.get("suggestedAlternative").asText();
        assertThat(suggested).startsWith("demo-restaurant-");
        assertThat(suggested.length()).isEqualTo("demo-restaurant-".length() + 4);

        // 3. Endpoint combinado /api/v1/auth/onboarding/check-availability con NIT ocupado ("12345678")
        mockMvc.perform(get("/api/v1/auth/onboarding/check-availability")
                .param("slug", freeSlug)
                .param("nit", "12345678"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false))
            .andExpect(jsonPath("$.slugAvailable").value(true))
            .andExpect(jsonPath("$.nitAvailable").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/tenants permite aprovisionamiento asistido para SUPERADMIN / ADMIN_TENANT con preset TRADITIONAL")
    void backofficeTenantProvisioningSuccess() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "casa-del-camba-" + suffix;

        String payload = """
            {
                "company": {
                    "legalName": "La Casa del Camba %s",
                    "slug": "%s",
                    "nitOrTaxId": "777%s"
                },
                "branch": {
                    "name": "Sucursal Urubó",
                    "address": "Av. Principal Urubó"
                },
                "businessTemplate": "TRADITIONAL",
                "includeDemoCatalog": true,
                "adminUser": {
                    "fullName": "Roberto Paz",
                    "username": "rpaz",
                    "email": "rpaz@casadelcamba.bo",
                    "password": "PasswordCamba123"
                }
            }
            """.formatted(suffix, slug, suffix.substring(0, 5));

        // Sin token debe rechazar con 401
        mockMvc.perform(post("/api/v1/backoffice/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized());

        // Con token de SUPERADMIN / ADMIN_TENANT debe crear con 201 Created
        Tenant demoTenant = tenantRepository.findBySlug("demo-restaurant").orElseThrow();
        String superAdminToken = "Bearer " + jwtService.generateToken(
            UUID.randomUUID(),
            "superadmin",
            "SUPERADMIN",
            demoTenant.getId(),
            null,
            demoTenant.getSlug(),
            SystemPermissions.ALL_PERMISSIONS
        );

        MvcResult res = mockMvc.perform(post("/api/v1/backoffice/tenants")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value(slug))
            .andReturn();

        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        UUID tenantId = UUID.fromString(node.get("tenantId").asText());
        UUID branchId = UUID.fromString(node.get("branchId").asText());

        // Verificar preset TRADITIONAL
        assertThat(configurationPublicApi.getString(SystemParameter.INVENTORY_DEDUCTION_MODE, branchId, tenantId))
            .isEqualTo("AUTOMATIC_STRICT");
        assertThat(configurationPublicApi.getString(SystemParameter.POS_DEFAULT_ORDER_MODE, branchId, tenantId))
            .isEqualTo("DINE_IN");
        assertThat(configurationPublicApi.getBoolean(SystemParameter.FINANCE_REQUIRE_SUPERVISOR_PIN_CANCEL, branchId, tenantId))
            .isTrue();
        assertThat(configurationPublicApi.getInt(SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES, branchId, tenantId))
            .isEqualTo(60);
        assertThat(catalogPublicApi.getActiveMenuForBranch(tenantId, branchId)).hasSize(3);
    }
}
