package com.restaurant.app.modules.company;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.JwtService;
import com.restaurant.app.core.security.SystemPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CompanyIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;
    private Tenant testTenant;
    private Branch testBranch;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testTenant = tenantRepository.save(new Tenant("Empresa Test " + uniqueSuffix, "NIT-" + uniqueSuffix, "empresa-" + uniqueSuffix));
        testBranch = branchRepository.save(new Branch(testTenant.getId(), "Sucursal Principal " + uniqueSuffix, "America/La_Paz"));

        AppUser admin = appUserRepository.save(new AppUser(
            testTenant.getId(),
            testBranch.getId(),
            "admin_" + uniqueSuffix,
            "admin_" + uniqueSuffix + "@test.com",
            "hashed_pwd",
            "ADMIN_TENANT"
        ));

        adminToken = "Bearer " + jwtService.generateToken(
            admin.getId(),
            admin.getUsername(),
            admin.getRole(),
            testTenant.getId(),
            testBranch.getId(),
            testTenant.getSlug(),
            SystemPermissions.ALL_PERMISSIONS
        );
    }

    @Test
    @DisplayName("GET /api/v1/company retorna datos de la empresa actual")
    void getCompanySuccess() throws Exception {
        mockMvc.perform(get("/api/v1/company")
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testTenant.getId().toString()))
            .andExpect(jsonPath("$.legalName").value(testTenant.getLegalName()))
            .andExpect(jsonPath("$.slug").value(testTenant.getSlug()));
    }

    @Test
    @DisplayName("PUT /api/v1/company actualiza datos fiscales y slug")
    void updateCompanySuccess() throws Exception {
        String newSlug = "don-pollo-" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
            {
                "legalName": "Restaurante Don Pollo Actualizado",
                "nitOrTaxId": "%s",
                "slug": "%s"
            }
            """.formatted(testTenant.getNitOrTaxId(), newSlug);

        mockMvc.perform(put("/api/v1/company")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.legalName").value("Restaurante Don Pollo Actualizado"))
            .andExpect(jsonPath("$.slug").value(newSlug));
    }

    @Test
    @DisplayName("GET /api/v1/branches retorna listado de sucursales del tenant")
    void getBranchesSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/branches")
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value(testBranch.getName()))
            .andExpect(jsonPath("$[0].timezone").value("America/La_Paz"));
    }

    @Test
    @DisplayName("POST /api/v1/branches crea sucursal con zona horaria válida")
    void createBranchSuccess() throws Exception {
        String body = """
            {
                "name": "Sucursal Sur Calacoto",
                "address": "Av. Ballivián Calle 15",
                "phone": "+591 2 2790000",
                "timezone": "America/La_Paz"
            }
            """;

        mockMvc.perform(post("/api/v1/branches")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Sucursal Sur Calacoto"))
            .andExpect(jsonPath("$.timezone").value("America/La_Paz"))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/branches falla con zona horaria IANA inválida")
    void createBranchFailsWithInvalidTimezone() throws Exception {
        String body = """
            {
                "name": "Sucursal Invalida",
                "address": "Calle Falsa 123",
                "phone": "123456",
                "timezone": "Zona/Invalida_Inventada"
            }
            """;

        mockMvc.perform(post("/api/v1/branches")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Solicitud inválida"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("PATCH /api/v1/branches/{id}/status conmuta estado de la sucursal")
    void toggleBranchStatusSuccess() throws Exception {
        String body = """
            {
                "isActive": false
            }
            """;

        mockMvc.perform(patch("/api/v1/branches/" + testBranch.getId() + "/status")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));

        // Revertir a activa
        mockMvc.perform(patch("/api/v1/branches/" + testBranch.getId() + "/status")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(true));
    }
}
