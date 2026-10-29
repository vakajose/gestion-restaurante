package com.restaurant.app.core.security;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.api.CreateUserRequest;
import com.restaurant.app.core.security.api.RolePublicApi;
import com.restaurant.app.core.security.api.RolePublicApi.AssignBranchesRequest;
import com.restaurant.app.core.security.api.RolePublicApi.ChangePasswordRequest;
import com.restaurant.app.core.security.api.RolePublicApi.CreateRoleRequest;
import com.restaurant.app.core.security.api.RolePublicApi.PermissionCatalogItemDto;
import com.restaurant.app.core.security.api.RolePublicApi.ResetPasswordRequest;
import com.restaurant.app.core.security.api.RolePublicApi.ResetPasswordResponse;
import com.restaurant.app.core.security.api.RolePublicApi.RoleDto;
import com.restaurant.app.core.security.api.RolePublicApi.UpdateRoleRequest;
import com.restaurant.app.core.security.api.UpdateUserRequest;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RbacDynamicRolesIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserPublicApi userPublicApi;

    @Autowired
    private RolePublicApi rolePublicApi;

    @Autowired
    private AuthService authService;

    private Tenant tenant;
    private Branch branchMain;
    private Branch branchNorth;
    private UserSummaryDto adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
        tenant = tenantRepository.save(new Tenant(
            "Grupo Gastronómico RBAC " + uniqueSuffix,
            "889900" + uniqueSuffix,
            "rbac-group-" + uniqueSuffix,
            null
        ));

        branchMain = branchRepository.save(new Branch(tenant.getId(), "Sucursal Central", "America/La_Paz"));
        branchNorth = branchRepository.save(new Branch(tenant.getId(), "Sucursal Norte", "America/La_Paz"));

        userPublicApi.seedDefaultRolesForTenant(tenant.getId());

        adminUser = userPublicApi.createUser(
            tenant.getId(),
            new CreateUserRequest(
                "admin_rbac_" + uniqueSuffix,
                "Admin Propietario",
                "admin@" + uniqueSuffix + ".bo",
                "AdminPass123!",
                "ADMIN_TENANT",
                branchMain.getId(),
                Set.of(branchMain.getId(), branchNorth.getId()),
                null
            )
        );

        LoginResponse loginResponse = authService.login(
            new LoginRequest(adminUser.username(), "AdminPass123!")
        );
        adminToken = loginResponse.accessToken();
    }

    @Test
    @DisplayName("1. Catálogo de permisos y listado de roles semilla incluye ADMIN_TENANT con comodín *")
    void shouldListSystemRolesAndPermissionsCatalog() throws Exception {
        MvcResult catalogRes = mockMvc.perform(get("/api/v1/roles/permissions-catalog")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();

        List<PermissionCatalogItemDto> catalog = objectMapper.readValue(
            catalogRes.getResponse().getContentAsString(),
            new TypeReference<>() {}
        );
        assertThat(catalog).isNotEmpty();
        assertThat(catalog).extracting(PermissionCatalogItemDto::code)
            .contains("pos:access", "pos:discount_apply", "roles:manage", "settings:write");

        MvcResult rolesRes = mockMvc.perform(get("/api/v1/roles")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();

        List<RoleDto> roles = objectMapper.readValue(
            rolesRes.getResponse().getContentAsString(),
            new TypeReference<>() {}
        );
        assertThat(roles).extracting(RoleDto::code)
            .contains("ADMIN_TENANT", "BRANCH_MANAGER", "CASHIER", "KITCHEN");

        RoleDto adminRole = roles.stream()
            .filter(r -> "ADMIN_TENANT".equals(r.code()))
            .findFirst()
            .orElseThrow();
        assertThat(adminRole.isSystem()).isTrue();
        assertThat(adminRole.permissions()).contains("*");
    }

    @Test
    @DisplayName("2. Ciclo completo de rol personalizado (JEFE_BARRA), asignación multi-sucursal y login con privilegios dinámicos")
    void shouldCreateCustomRoleAssignUserBranchesAndAuthenticate() throws Exception {
        CreateRoleRequest createRoleReq = new CreateRoleRequest(
            "JEFE_BARRA",
            "Jefe de Barra y Coctelería",
            "Autoriza descuentos en barra y supervisa KDS",
            Set.of("pos:access", "pos:order_create", "pos:discount_apply", "pos:kds", "catalog:read")
        );

        MvcResult createRoleRes = mockMvc.perform(post("/api/v1/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRoleReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("JEFE_BARRA"))
            .andExpect(jsonPath("$.isSystem").value(false))
            .andReturn();

        RoleDto createdRole = objectMapper.readValue(
            createRoleRes.getResponse().getContentAsString(),
            RoleDto.class
        );
        assertThat(createdRole.permissions()).containsExactlyInAnyOrder(
            "pos:access", "pos:order_create", "pos:discount_apply", "pos:kds", "catalog:read"
        );

        // Crear usuario con el nuevo rol personalizado JEFE_BARRA
        CreateUserRequest barmanReq = new CreateUserRequest(
            "barman_carlos",
            "Carlos Mendoza",
            "carlos@barra.bo",
            "Barman123!",
            "JEFE_BARRA",
            branchMain.getId(),
            Set.of(branchMain.getId()),
            Instant.now().plus(30, ChronoUnit.DAYS)
        );

        MvcResult userRes = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(barmanReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("JEFE_BARRA"))
            .andReturn();

        UserSummaryDto barman = objectMapper.readValue(
            userRes.getResponse().getContentAsString(),
            UserSummaryDto.class
        );

        // Asignar sucursal adicional (Multi-sucursal N:M en user_branches)
        AssignBranchesRequest assignBranchesReq = new AssignBranchesRequest(
            branchMain.getId(),
            Set.of(branchMain.getId(), branchNorth.getId())
        );

        mockMvc.perform(post("/api/v1/users/" + barman.id() + "/branches")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignBranchesReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorizedBranchIds.length()").value(2));

        // Login del usuario con rol personalizado JEFE_BARRA
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("barman_carlos", "Barman123!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("JEFE_BARRA"))
            .andReturn();

        LoginResponse barmanLogin = objectMapper.readValue(
            loginRes.getResponse().getContentAsString(),
            LoginResponse.class
        );
        assertThat(barmanLogin.user().permissions()).containsExactlyInAnyOrder(
            "pos:access", "pos:order_create", "pos:discount_apply", "pos:kds", "catalog:read"
        );
        assertThat(barmanLogin.user().authorizedBranchIds()).containsExactlyInAnyOrder(
            branchMain.getId(), branchNorth.getId()
        );

        // No debe permitir eliminar un rol mientras tenga usuarios activos asignados (409 Conflict)
        mockMvc.perform(delete("/api/v1/roles/" + createdRole.id())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict());

        // Cambiar al usuario a CASHIER y luego eliminar el rol personalizado exitosamente
        userPublicApi.updateUser(
            tenant.getId(),
            barman.id(),
            new UpdateUserRequest(
                "barman_carlos",
                "Carlos Mendoza",
                "carlos@barra.bo",
                "CASHIER",
                branchMain.getId(),
                true,
                null
            )
        );

        mockMvc.perform(delete("/api/v1/roles/" + createdRole.id())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("3. Protección inmutable del rol ADMIN_TENANT frente a edición o eliminación")
    void shouldProtectAdminTenantRoleFromModificationOrDeletion() throws Exception {
        RoleDto adminRole = rolePublicApi.listRoles(tenant.getId()).stream()
            .filter(r -> "ADMIN_TENANT".equals(r.code()))
            .findFirst()
            .orElseThrow();

        UpdateRoleRequest updateAttempt = new UpdateRoleRequest(
            "Admin Recortado",
            "Intento de quitar permisos al propietario",
            Set.of("pos:access")
        );

        mockMvc.perform(put("/api/v1/roles/" + adminRole.id())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateAttempt)))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/v1/roles/" + adminRole.id())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("4. Vigencia temporal de rol (role_valid_until) rechaza login cuando ha expirado")
    void shouldRejectLoginWhenRoleValidUntilHasExpired() throws Exception {
        CreateUserRequest expiredAuditorReq = new CreateUserRequest(
            "auditor_externo",
            "Auditor Temporal",
            "auditor@externo.bo",
            "Auditor123!",
            "BRANCH_MANAGER",
            branchMain.getId(),
            Set.of(branchMain.getId()),
            Instant.now().minus(1, ChronoUnit.DAYS)
        );

        userPublicApi.createUser(tenant.getId(), expiredAuditorReq);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("auditor_externo", "Auditor123!"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Jerarquía de restablecimiento de contraseña y cambio obligatorio (must_change_password)")
    void shouldEnforcePasswordResetHierarchyAndMustChangePasswordFlow() throws Exception {
        UserSummaryDto manager = userPublicApi.createUser(
            tenant.getId(),
            new CreateUserRequest(
                "gerente_sucursal",
                "Gerente Sucursal",
                "gerente@rest.bo",
                "Gerente123!",
                "BRANCH_MANAGER",
                branchMain.getId()
            )
        );
        UserSummaryDto cashier = userPublicApi.createUser(
            tenant.getId(),
            new CreateUserRequest(
                "cajero_turno",
                "Cajero Turno",
                "cajero@rest.bo",
                "Cajero123!",
                "CASHIER",
                branchMain.getId()
            )
        );

        String managerToken = authService.login(new LoginRequest("gerente_sucursal", "Gerente123!")).accessToken();

        // 5.1 BRANCH_MANAGER NO puede resetear la contraseña de ADMIN_TENANT (403 Forbidden)
        mockMvc.perform(post("/api/v1/users/" + adminUser.id() + "/reset-password")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest("HackAttempt123!"))))
            .andExpect(status().isForbidden());

        // 5.2 BRANCH_MANAGER SÍ puede resetear la contraseña de un CASHIER de su sucursal
        MvcResult resetRes = mockMvc.perform(post("/api/v1/users/" + cashier.id() + "/reset-password")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest("TempClave999!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(true))
            .andReturn();

        ResetPasswordResponse resetDto = objectMapper.readValue(
            resetRes.getResponse().getContentAsString(),
            ResetPasswordResponse.class
        );
        assertThat(resetDto.mustChangePassword()).isTrue();

        // 5.3 Al iniciar sesión con la contraseña temporal, el flag mustChangePassword viene en true
        MvcResult cashierLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("cajero_turno", "TempClave999!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.mustChangePassword").value(true))
            .andReturn();

        LoginResponse cashierLogin = objectMapper.readValue(
            cashierLoginRes.getResponse().getContentAsString(),
            LoginResponse.class
        );

        // 5.4 El cajero actualiza su propia contraseña y limpia mustChangePassword = false
        mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + cashierLogin.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new ChangePasswordRequest("TempClave999!", "NuevaClaveSegura2026!")
                )))
            .andExpect(status().isOk());

        UserSummaryDto updatedCashier = userPublicApi.getUserById(tenant.getId(), cashier.id());
        assertThat(updatedCashier.mustChangePassword()).isFalse();
    }
}
