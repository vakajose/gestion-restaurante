package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.AdjustQuotaRequest;
import com.restaurant.app.modules.catalog.api.BranchServiceSessionDto;
import com.restaurant.app.modules.catalog.api.ConfigureServiceSessionRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.ShiftMenuDto;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/menus/{id}")
    public DailyMenuDto updateMenu(@PathVariable UUID id, @Valid @RequestBody CreateDailyMenuRequest request) {
        return dailyMenuService.updateMenu(currentTenantId(), id, request);
    }

    @DeleteMapping("/menus/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenu(@PathVariable UUID id) {
        dailyMenuService.deleteMenu(currentTenantId(), id);
    }

    public record CloneDailyMenuRequest(UUID targetBranchId, String newName) {}

    @PostMapping("/menus/{id}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public DailyMenuDto cloneMenu(@PathVariable UUID id, @RequestBody(required = false) CloneDailyMenuRequest request) {
        return dailyMenuService.cloneMenu(
            currentTenantId(),
            id,
            request != null ? request.targetBranchId() : null,
            request != null ? request.newName() : null
        );
    }

    @GetMapping("/service-sessions/current")
    public ResponseEntity<BranchServiceSessionDto> getCurrentServiceSession(@RequestParam(required = false) UUID branchId) {
        UUID effectiveBranchId = branchId != null ? branchId : TenantContextHolder.getBranchId();
        if (effectiveBranchId == null) {
            return ResponseEntity.noContent().build();
        }
        return dailyMenuService.getActiveServiceSession(currentTenantId(), effectiveBranchId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/service-sessions/configure")
    public BranchServiceSessionDto configureServiceSession(@Valid @RequestBody ConfigureServiceSessionRequest request) {
        return dailyMenuService.configureServiceSession(currentTenantId(), currentUserId(), request);
    }

    @PostMapping("/service-sessions/{sessionId}/close")
    public BranchServiceSessionDto closeServiceSession(@PathVariable UUID sessionId) {
        return dailyMenuService.closeServiceSession(currentTenantId(), sessionId);
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
            .orElseGet(() -> ResponseEntity.noContent().build());
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
