package com.restaurant.app.modules.pos;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.CreatePromotionRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.PromotionDto;
import com.restaurant.app.modules.pos.internal.PromotionEngine;
import com.restaurant.app.modules.pos.internal.PromotionService;
import org.junit.jupiter.api.AfterEach;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PromotionIntegrationTests {

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

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private PromotionEngine promotionEngine;

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

        TenantContextHolder.set(new TenantContext(tenantId, branchId, UUID.randomUUID(), "admin", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Debe crear una promoción vía REST y recuperarla con GET /api/v1/pos/promotions")
    void testCreatePromotionViaRest() throws Exception {
        String promoJson = String.format("""
            {
                "name": "Happy Hour 20%% OFF",
                "description": "Descuento del 20%% en bebidas",
                "discountType": "PERCENTAGE",
                "discountValue": 20.00,
                "scope": "GLOBAL",
                "applicationMode": "AUTOMATIC",
                "daysOfWeek": "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
                "startTime": "17:00:00",
                "endTime": "20:00:00",
                "minOrderAmount": 50.00,
                "branchIds": ["%s"]
            }
            """, branchId);

        mockMvc.perform(post("/api/v1/pos/promotions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(promoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Happy Hour 20% OFF"))
                .andExpect(jsonPath("$.discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.discountValue").value(20.00))
                .andExpect(jsonPath("$.minOrderAmount").value(50.00));

        mockMvc.perform(get("/api/v1/pos/promotions")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Debe evaluar correctamente la vigencia temporal según la zona horaria de la sucursal (ADR-0006)")
    void testPromotionActiveEvaluationWithBranchTimezone() {
        ZonedDateTime laPazNow = Instant.now().atZone(ZoneId.of("America/La_Paz"));
        LocalTime currentTime = laPazNow.toLocalTime();
        DayOfWeek currentDay = laPazNow.getDayOfWeek();

        // 1. Promoción activa que incluye el día y hora actual de La Paz
        CreatePromotionRequest activeReq = new CreatePromotionRequest(
                "Promo Ahora",
                "Activa en la hora actual",
                "PERCENTAGE",
                BigDecimal.valueOf(15),
                "GLOBAL",
                null,
                null,
                "AUTOMATIC",
                currentDay.name(),
                currentTime.minusHours(1),
                currentTime.plusHours(1),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                BigDecimal.ZERO,
                Set.of(branchId)
        );
        PromotionDto createdActive = promotionService.createPromotion(tenantId, activeReq);

        List<PromotionDto> activeList = promotionService.getActivePromotionsForBranch(tenantId, branchId, Instant.now());
        assertThat(activeList.stream().anyMatch(p -> p.id().equals(createdActive.id()))).isTrue();

        // 2. Promoción inactiva fuera de la ventana horaria (ayer o hace 3 horas)
        CreatePromotionRequest inactiveReq = new CreatePromotionRequest(
                "Promo Inactiva",
                "Fuera de horario",
                "PERCENTAGE",
                BigDecimal.valueOf(30),
                "GLOBAL",
                null,
                null,
                "AUTOMATIC",
                currentDay.name(),
                currentTime.minusHours(4),
                currentTime.minusHours(2),
                LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(1),
                BigDecimal.ZERO,
                Set.of(branchId)
        );
        PromotionDto createdInactive = promotionService.createPromotion(tenantId, inactiveReq);

        List<PromotionDto> activeListAfter = promotionService.getActivePromotionsForBranch(tenantId, branchId, Instant.now());
        assertThat(activeListAfter.stream().anyMatch(p -> p.id().equals(createdInactive.id()))).isFalse();
    }

    @Test
    @DisplayName("Debe aplicar descuento automático en POS cuando la promoción está activa en el horario de sucursal")
    void testAutomaticPromotionAppliedInPosOrder() throws Exception {
        ZonedDateTime laPazNow = Instant.now().atZone(ZoneId.of("America/La_Paz"));
        LocalTime currentTime = laPazNow.toLocalTime();
        DayOfWeek currentDay = laPazNow.getDayOfWeek();

        // Crear promoción automática del 20% global
        CreatePromotionRequest autoPromoReq = new CreatePromotionRequest(
                "Auto Happy Hour 20%",
                "20% de descuento en el horario actual",
                "PERCENTAGE",
                BigDecimal.valueOf(20),
                "GLOBAL",
                null,
                null,
                "AUTOMATIC",
                currentDay.name(),
                currentTime.minusHours(1),
                currentTime.plusHours(1),
                null,
                null,
                BigDecimal.ZERO,
                Set.of(branchId)
        );
        promotionService.createPromotion(tenantId, autoPromoReq);

        // Vender 2 unidades de plato a Bs. 35.00 c/u (Subtotal = 70.00). Con 20% OFF, Total = 56.00
        String orderJson = String.format("""
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "paymentMethod": "CASH",
                "notes": "Venta con Happy Hour automático",
                "items": [
                    {
                        "dishId": "%s",
                        "quantity": 2,
                        "unitPrice": 35.00
                    }
                ]
            }
            """, UUID.randomUUID(), branchId, demoDishId);

        MvcResult result = mockMvc.perform(post("/api/v1/pos/orders")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal totalAmount = new BigDecimal(json.get("totalAmount").asText());
        // 70.00 - 20% (14.00) = 56.00
        assertThat(totalAmount).isEqualByComparingTo(BigDecimal.valueOf(56.00));
        assertThat(json.get("items").get(0).get("discountAmount").asDouble()).isEqualTo(14.00);
    }

    @Test
    @DisplayName("Debe permitir al cajero aplicar manualmente una promoción parametrizable")
    void testManualPromotionAppliedByCashier() throws Exception {
        // Promoción manual de monto fijo Bs. 15.00
        CreatePromotionRequest manualPromoReq = new CreatePromotionRequest(
                "Cupón Descuento Fijo",
                "Descuento de 15 Bs aplicable por cajero",
                "FIXED_AMOUNT",
                BigDecimal.valueOf(15),
                "GLOBAL",
                null,
                null,
                "MANUAL",
                null,
                null,
                null,
                null,
                null,
                BigDecimal.valueOf(30),
                Set.of(branchId)
        );
        PromotionDto promo = promotionService.createPromotion(tenantId, manualPromoReq);

        // Orden de 2 platos a Bs. 35 = 70 Bs. Con cupón manual de 15 Bs -> Total = 55.00
        String orderJson = String.format("""
            {
                "clientTransactionId": "%s",
                "branchId": "%s",
                "paymentMethod": "CASH",
                "appliedPromotionId": "%s",
                "items": [
                    {
                        "dishId": "%s",
                        "quantity": 2,
                        "unitPrice": 35.00
                    }
                ]
            }
            """, UUID.randomUUID(), branchId, promo.id(), demoDishId);

        MvcResult result = mockMvc.perform(post("/api/v1/pos/orders")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal totalAmount = new BigDecimal(json.get("totalAmount").asText());
        // 70.00 - 15.00 = 55.00
        assertThat(totalAmount).isEqualByComparingTo(BigDecimal.valueOf(55.00));
    }
}
