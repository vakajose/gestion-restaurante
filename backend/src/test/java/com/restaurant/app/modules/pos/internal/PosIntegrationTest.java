package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PosIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    private MockMvc mockMvc;
    private String jwtToken;
    private UUID tenantId;
    private UUID branchId;
    private UUID demoDishId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Login as demo admin to obtain JWT
        String loginJson = """
            {
                "login": "admin",
                "password": "admin123"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        this.jwtToken = responseNode.get("token").asText();

        Tenant tenant = tenantRepository.findByNitOrTaxId("12345678").orElseThrow();
        this.tenantId = tenant.getId();

        Branch branch = branchRepository.findByTenantId(tenantId).getFirst();
        this.branchId = branch.getId();

        DishPriceDto dish = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId).getFirst();
        this.demoDishId = dish.dishId();
    }

    @Test
    @DisplayName("Debe rechazar peticiones no autenticadas a /api/v1/pos/orders con 401")
    void shouldRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/pos/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe crear orden exitosa retornando 201 CREATED con ticket T-1001 y subtotales calculados")
    void shouldCreateOrderSuccessfully() throws Exception {
        UUID clientTxId = UUID.randomUUID();

        String requestJson = String.format("""
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "paymentMethod": "CASH",
                "notes": "Cliente regular",
                "items": [
                    {
                        "dishId": "%s",
                        "quantity": 2,
                        "unitPrice": 35.00,
                        "notes": "Bien cocida"
                    }
                ]
            }
            """, clientTxId, branchId, demoDishId);

        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.ticketNumber").value(startsWith("T-")))
            .andExpect(jsonPath("$.orderStatus").value("PAID"))
            .andExpect(jsonPath("$.paymentMethod").value("CASH"))
            .andExpect(jsonPath("$.totalAmount").value(70.00))
            .andExpect(jsonPath("$.clientTransactionId").value(clientTxId.toString()))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].subtotal").value(70.00))
            .andExpect(jsonPath("$.items[0].dishName").isNotEmpty());
    }

    @Test
    @DisplayName("Idempotencia REST: segunda llamada con el mismo clientTransactionId retorna 200 OK con la misma orden")
    void shouldBeIdempotentOnSecondCall() throws Exception {
        UUID clientTxId = UUID.randomUUID();

        String requestJson = String.format("""
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "paymentMethod": "CARD",
                "notes": "Cobro con tarjeta POS",
                "items": [
                    {
                        "dishId": "%s",
                        "quantity": 1,
                        "unitPrice": 35.00
                    }
                ]
            }
            """, clientTxId, branchId, demoDishId);

        // Primera llamada: 201 CREATED
        MvcResult firstResult = mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode firstOrder = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        String orderId = firstOrder.get("id").asText();

        // Segunda llamada idéntica (usando también header X-Client-Transaction-Id opcional): 200 OK
        MvcResult secondResult = mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-Client-Transaction-Id", clientTxId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode secondOrder = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        assertThat(secondOrder.get("id").asText()).isEqualTo(orderId);
        assertThat(secondOrder.get("ticketNumber").asText()).isEqualTo(firstOrder.get("ticketNumber").asText());
    }

    @Test
    @DisplayName("Debe sincronizar lote de órdenes offline mediante /api/v1/pos/orders/sync")
    void shouldSyncOfflineOrdersBatch() throws Exception {
        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();

        String syncJson = String.format("""
            {
                "orders": [
                    {
                        "clientTransactionId": "%s",
                        "branchId": "%s",
                        "paymentMethod": "CASH",
                        "items": [
                            {
                                "dishId": "%s",
                                "quantity": 1,
                                "unitPrice": 35.00
                            }
                        ]
                    },
                    {
                        "clientTransactionId": "%s",
                        "branchId": "%s",
                        "paymentMethod": "QR",
                        "items": [
                            {
                                "dishId": "%s",
                                "quantity": 2,
                                "unitPrice": 12.00
                            }
                        ]
                    }
                ]
            }
            """, tx1, branchId, demoDishId, tx2, branchId, demoDishId);

        mockMvc.perform(post("/api/v1/pos/orders/sync")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalReceived").value(2))
            .andExpect(jsonPath("$.totalProcessed").value(2))
            .andExpect(jsonPath("$.syncedOrders", hasSize(2)));
    }

    @Test
    @DisplayName("Debe listar órdenes y filtrar por branchId y fecha")
    void shouldListAndFilterOrders() throws Exception {
        UUID clientTxId = UUID.randomUUID();
        String createJson = String.format("""
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "paymentMethod": "CASH",
                "items": [
                    {
                        "dishId": "%s",
                        "quantity": 1,
                        "unitPrice": 35.00
                    }
                ]
            }
            """, clientTxId, branchId, demoDishId);

        mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated());

        // Listar sin filtros
        mockMvc.perform(get("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // Filtrar por branchId
        mockMvc.perform(get("/api/v1/pos/orders")
                .param("branchId", branchId.toString())
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // Filtrar por fecha de hoy
        mockMvc.perform(get("/api/v1/pos/orders")
                .param("branchId", branchId.toString())
                .param("date", LocalDate.now().toString())
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Debe obtener orden por ID y retornar 404 para orden inexistente")
    void shouldGetOrderByIdAndHandle404() throws Exception {
        UUID clientTxId = UUID.randomUUID();
        String createJson = String.format("""
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "paymentMethod": "CASH",
                "items": [
                    {
                        "dishId": "%s",
                        "quantity": 1,
                        "unitPrice": 35.00
                    }
                ]
            }
            """, clientTxId, branchId, demoDishId);

        MvcResult result = mockMvc.perform(post("/api/v1/pos/orders")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        String orderId = created.get("id").asText();

        // 200 OK con la orden
        mockMvc.perform(get("/api/v1/pos/orders/" + orderId)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.ticketNumber").value(created.get("ticketNumber").asText()));

        // 404 Not Found para UUID aleatorio
        mockMvc.perform(get("/api/v1/pos/orders/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
