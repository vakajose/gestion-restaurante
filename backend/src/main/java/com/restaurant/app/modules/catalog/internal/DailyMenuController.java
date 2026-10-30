package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.AdjustQuotaRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.ShiftMenuDto;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
class DailyMenuController {

    private final DailyMenuPublicApi dailyMenuService;

    DailyMenuController(DailyMenuPublicApi dailyMenuService) {
        this.dailyMenuService = dailyMenuService;
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedOperationException("Contexto de tenant requerido para esta operación");
        }
        return tenantId;
    }

    private UUID currentUserId() {
        UUID userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedOperationException("Contexto de usuario requerido para esta operación");
        }
        return userId;
    }

    @PostMapping("/menus")
    @ResponseStatus(HttpStatus.CREATED)
    public DailyMenuDto createMenu(@Valid @RequestBody CreateDailyMenuRequest request) {
        return dailyMenuService.createMenu(currentTenantId(), request);
    }

    @GetMapping("/menus")
    public List<DailyMenuDto> getMenus(
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) Boolean templateOnly
    ) {
        return dailyMenuService.findMenus(currentTenantId(), branchId, templateOnly);
    }

    @GetMapping("/menus/{id}")
    public DailyMenuDto getMenuById(@PathVariable UUID id) {
        return dailyMenuService.getMenuById(id);
    }

    public record InstantiateShiftMenuRequest(
        @NotNull(message = "El menuTemplateId es obligatorio")
        UUID menuTemplateId,
        UUID branchId,
        Map<UUID, Integer> customQuotas
    ) {}

    @PostMapping("/shifts/{shiftId}/menu/instantiate")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftMenuDto instantiateShiftMenu(
        @PathVariable UUID shiftId,
        @Valid @RequestBody InstantiateShiftMenuRequest request
    ) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();
        return dailyMenuService.instantiateTemplateForShift(
            tenantId,
            request.branchId(),
            shiftId,
            request.menuTemplateId(),
            request.customQuotas(),
            userId
        );
    }

    @GetMapping("/shifts/{shiftId}/menu")
    public ResponseEntity<ShiftMenuDto> getShiftMenu(@PathVariable UUID shiftId) {
        return dailyMenuService.getShiftMenu(shiftId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("Menú de Turno", shiftId));
    }

    @PostMapping("/shifts/{shiftId}/menu-items/{itemId}/adjust-quota")
    public ShiftMenuItemDto adjustQuota(
        @PathVariable UUID shiftId,
        @PathVariable UUID itemId,
        @Valid @RequestBody AdjustQuotaRequest request
    ) {
        return dailyMenuService.adjustQuota(shiftId, itemId, request.quantityDelta(), request.reason(), currentUserId());
    }
}
