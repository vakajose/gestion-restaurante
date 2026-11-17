package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ItemSoldOutException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.BranchServiceSessionDto;
import com.restaurant.app.modules.catalog.api.ComboDto;
import com.restaurant.app.modules.catalog.api.ComboItemDto;
import com.restaurant.app.modules.catalog.api.ComboItemOptionDto;
import com.restaurant.app.modules.catalog.api.ConfigureServiceSessionRequest;
import com.restaurant.app.modules.catalog.api.CourseType;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuItemRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuItemDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.ShiftMenuDto;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
class DailyMenuServiceImpl implements DailyMenuPublicApi {

    private final DailyMenuRepository dailyMenuRepository;
    private final DishRepository dishRepository;
    private final ComboRepository comboRepository;
    private final ShiftMenuAssignmentRepository shiftMenuAssignmentRepository;
    private final ShiftMenuItemRepository shiftMenuItemRepository;
    private final ShiftMenuQuotaAdjustmentRepository shiftMenuQuotaAdjustmentRepository;
    private final BranchServiceSessionRepository branchServiceSessionRepository;
    private final BranchPublicApi branchPublicApi;
    private final JdbcClient jdbcClient;

    DailyMenuServiceImpl(
        DailyMenuRepository dailyMenuRepository,
        DishRepository dishRepository,
        ComboRepository comboRepository,
        ShiftMenuAssignmentRepository shiftMenuAssignmentRepository,
        ShiftMenuItemRepository shiftMenuItemRepository,
        ShiftMenuQuotaAdjustmentRepository shiftMenuQuotaAdjustmentRepository,
        BranchServiceSessionRepository branchServiceSessionRepository,
        BranchPublicApi branchPublicApi,
        JdbcClient jdbcClient
    ) {
        this.dailyMenuRepository = dailyMenuRepository;
        this.dishRepository = dishRepository;
        this.comboRepository = comboRepository;
        this.shiftMenuAssignmentRepository = shiftMenuAssignmentRepository;
        this.shiftMenuItemRepository = shiftMenuItemRepository;
        this.shiftMenuQuotaAdjustmentRepository = shiftMenuQuotaAdjustmentRepository;
        this.branchServiceSessionRepository = branchServiceSessionRepository;
        this.branchPublicApi = branchPublicApi;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public DailyMenuDto createMenu(UUID tenantId, CreateDailyMenuRequest request) {
        DailyMenu menu = new DailyMenu(
            tenantId,
            request.branchId(),
            request.name().trim(),
            request.description(),
            request.dayOfWeek(),
            request.priceMode(),
            request.packagePrice(),
            request.isTemplate(),
            true
        );

        applyMenuItemsAndCombos(tenantId, menu, request);

        DailyMenu saved = dailyMenuRepository.save(menu);
        return toDailyMenuDto(saved);
    }

    @Override
    public DailyMenuDto updateMenu(UUID tenantId, UUID menuId, CreateDailyMenuRequest request) {
        DailyMenu menu = dailyMenuRepository.findByIdAndTenantId(menuId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Menú Diario", menuId));

        menu.setBranchId(request.branchId());
        menu.setName(request.name().trim());
        menu.setDescription(request.description());
        menu.setDayOfWeek(request.dayOfWeek());
        menu.setPriceMode(request.priceMode());
        menu.setPackagePrice(request.packagePrice());
        menu.setTemplate(request.isTemplate());
        menu.getItems().clear();
        menu.getCombos().clear();
        dailyMenuRepository.flush();

        applyMenuItemsAndCombos(tenantId, menu, request);

        DailyMenu saved = dailyMenuRepository.save(menu);
        return toDailyMenuDto(saved);
    }

    @Override
    public void deleteMenu(UUID tenantId, UUID menuId) {
        DailyMenu menu = dailyMenuRepository.findByIdAndTenantId(menuId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Menú Diario", menuId));
        menu.setActive(false);
        dailyMenuRepository.save(menu);
    }

    @Override
    public DailyMenuDto cloneMenu(UUID tenantId, UUID menuId, UUID targetBranchId, String newName) {
        DailyMenu source = dailyMenuRepository.findByIdAndTenantId(menuId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Menú Diario", menuId));

        String resolvedName = (newName != null && !newName.isBlank()) ? newName.trim() : source.getName() + " (Copia)";
        UUID resolvedBranchId = targetBranchId != null ? targetBranchId : source.getBranchId();

        DailyMenu copy = new DailyMenu(
            tenantId,
            resolvedBranchId,
            resolvedName,
            source.getDescription(),
            source.getDayOfWeek(),
            source.getPriceMode(),
            source.getPackagePrice(),
            source.isTemplate(),
            true
        );

        for (DailyMenuItem srcItem : source.getItems()) {
            DailyMenuItem itemCopy = new DailyMenuItem(
                copy,
                srcItem.getDish(),
                srcItem.getCourseType(),
                srcItem.getSuggestedQuota(),
                srcItem.getOverridePrice(),
                srcItem.isMandatoryInPackage(),
                srcItem.getSortOrder()
            );
            itemCopy.setAllowIndividualSale(srcItem.isAllowIndividualSale());
            itemCopy.setUnlimited(srcItem.isUnlimited());
            copy.addItem(itemCopy);
        }

        for (DailyMenuCombo srcCombo : source.getCombos()) {
            copy.addCombo(new DailyMenuCombo(copy, srcCombo.getComboId(), srcCombo.getSortOrder()));
        }

        return toDailyMenuDto(dailyMenuRepository.save(copy));
    }

    private void applyMenuItemsAndCombos(UUID tenantId, DailyMenu menu, CreateDailyMenuRequest request) {
        Set<CourseType> presentCourses = new HashSet<>();
        if (request.items() != null) {
            for (CreateDailyMenuItemRequest itemReq : request.items()) {
                Dish dish = dishRepository.findByIdAndTenantId(itemReq.dishId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Plato", itemReq.dishId()));

                DailyMenuItem item = new DailyMenuItem(
                    menu,
                    dish,
                    itemReq.courseType(),
                    itemReq.suggestedQuota(),
                    itemReq.overridePrice(),
                    itemReq.isMandatoryInPackage(),
                    itemReq.sortOrder()
                );
                item.setAllowIndividualSale(itemReq.resolvedAllowIndividualSale());
                item.setUnlimited(itemReq.resolvedIsUnlimited());
                menu.addItem(item);
                if (itemReq.courseType() != null) {
                    presentCourses.add(itemReq.courseType());
                }
            }
        }

        if (request.comboIds() != null && !request.comboIds().isEmpty()) {
            int comboOrder = 0;
            Set<UUID> seenCombos = new HashSet<>();
            for (UUID comboId : request.comboIds()) {
                if (comboId == null || !seenCombos.add(comboId)) {
                    continue;
                }
                Combo combo = comboRepository.findByIdAndTenantId(comboId, tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Combo", comboId));
                for (ComboItem ci : combo.getItems()) {
                    if ("MENU_COURSE".equalsIgnoreCase(ci.getSelectionSource()) && ci.getCourseType() != null) {
                        if (!presentCourses.contains(ci.getCourseType())) {
                            throw new BusinessRuleException(
                                "El combo armable '" + combo.getName() + "' requiere al menos un plato del tiempo '" +
                                ci.getCourseType() + "' dentro del menú."
                            );
                        }
                    }
                }
                menu.addCombo(new DailyMenuCombo(menu, combo.getId(), comboOrder++));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMenuDto> findMenus(UUID tenantId, UUID branchId, Boolean templateOnly) {
        return dailyMenuRepository.findMenus(tenantId, branchId, templateOnly).stream()
            .map(this::toDailyMenuDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DailyMenuDto getMenuById(UUID menuId) {
        return dailyMenuRepository.findById(menuId)
            .map(this::toDailyMenuDto)
            .orElseThrow(() -> new EntityNotFoundException("Menú Diario", menuId));
    }

    @Override
    public BranchServiceSessionDto configureServiceSession(
        UUID tenantId,
        UUID userId,
        ConfigureServiceSessionRequest request
    ) {
        UUID branchId = request.branchId();
        String mode = request.resolvedMenuMode();
        if (!"FULL_CATALOG".equals(mode) && !"DAILY_MENU".equals(mode)) {
            throw new ValidationException("Modo de menú inválido: " + mode + ". Valores permitidos: FULL_CATALOG, DAILY_MENU");
        }
        if ("DAILY_MENU".equals(mode) && request.dailyMenuId() == null) {
            throw new ValidationException("Debe seleccionar una plantilla de Menú del Día para el modo DAILY_MENU");
        }

        LocalDate serviceDate = resolveBranchServiceDate(branchId);

        BranchServiceSession session = branchServiceSessionRepository.findOpenSessionByBranch(tenantId, branchId)
            .orElseGet(() -> new BranchServiceSession(
                tenantId,
                branchId,
                serviceDate,
                mode,
                "DAILY_MENU".equals(mode) ? request.dailyMenuId() : null,
                "CONFIGURED",
                userId,
                request.notes()
            ));

        session.setMenuMode(mode);
        session.setDailyMenuId("DAILY_MENU".equals(mode) ? request.dailyMenuId() : null);
        session.setConfiguredBy(userId);
        session.setNotes(request.notes());
        session = branchServiceSessionRepository.save(session);

        // Si ya existía una asignación previa para esta sesión, reemplazar su snapshot
        Optional<ShiftMenuAssignment> existingAssignmentOpt = shiftMenuAssignmentRepository.findByServiceSessionId(session.getId());
        if (existingAssignmentOpt.isPresent()) {
            shiftMenuAssignmentRepository.delete(existingAssignmentOpt.get());
            shiftMenuAssignmentRepository.flush();
        }

        if ("DAILY_MENU".equals(mode)) {
            instantiateForServiceSession(
                tenantId,
                branchId,
                session.getId(),
                null,
                request.dailyMenuId(),
                request.customQuotas(),
                request.unlimitedDishIds(),
                userId
            );
        }

        return toServiceSessionDto(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchServiceSessionDto> getActiveServiceSession(UUID tenantId, UUID branchId) {
        return branchServiceSessionRepository.findOpenSessionByBranch(tenantId, branchId)
            .map(this::toServiceSessionDto);
    }

    @Override
    public BranchServiceSessionDto attachCashShiftToServiceSession(
        UUID tenantId,
        UUID branchId,
        UUID cashShiftId,
        UUID menuTemplateId,
        Map<UUID, Integer> customQuotas,
        UUID openedBy,
        boolean allowAutoFullCatalog
    ) {
        Optional<BranchServiceSession> existingOpt = branchServiceSessionRepository.findOpenSessionByBranch(tenantId, branchId);
        BranchServiceSession session;

        if (menuTemplateId != null) {
            // Retrocompatibilidad: apertura directa especificando un menuTemplateId
            if (existingOpt.isEmpty()) {
                session = branchServiceSessionRepository.save(new BranchServiceSession(
                    tenantId,
                    branchId,
                    resolveBranchServiceDate(branchId),
                    "DAILY_MENU",
                    menuTemplateId,
                    "ACTIVE",
                    openedBy,
                    null
                ));
                instantiateForServiceSession(tenantId, branchId, session.getId(), cashShiftId, menuTemplateId, customQuotas, List.of(), openedBy);
            } else {
                session = existingOpt.get();
                if (!"ACTIVE".equals(session.getStatus())) {
                    session.setStatus("ACTIVE");
                    session.setActivatedAt(Instant.now());
                    session = branchServiceSessionRepository.save(session);
                }
            }
            return toServiceSessionDto(session);
        }

        if (existingOpt.isEmpty()) {
            if (!allowAutoFullCatalog) {
                throw new BusinessRuleException(
                    "El administrador aún no ha configurado la jornada ni el menú del día para esta sucursal."
                );
            }
            session = branchServiceSessionRepository.save(new BranchServiceSession(
                tenantId,
                branchId,
                resolveBranchServiceDate(branchId),
                "FULL_CATALOG",
                null,
                "ACTIVE",
                openedBy,
                "Jornada automática Carta Completa"
            ));
            return toServiceSessionDto(session);
        }

        session = existingOpt.get();
        if (!"ACTIVE".equals(session.getStatus())) {
            session.setStatus("ACTIVE");
            session.setActivatedAt(Instant.now());
            session = branchServiceSessionRepository.save(session);
        }
        return toServiceSessionDto(session);
    }

    @Override
    public BranchServiceSessionDto closeServiceSession(UUID tenantId, UUID sessionId) {
        BranchServiceSession session = branchServiceSessionRepository.findByIdAndTenantId(sessionId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Jornada de Sucursal", sessionId));
        if ("CLOSED".equals(session.getStatus())) {
            return toServiceSessionDto(session);
        }
        session.setStatus("CLOSED");
        session.setClosedAt(Instant.now());
        return toServiceSessionDto(branchServiceSessionRepository.save(session));
    }

    @Override
    public ShiftMenuDto instantiateTemplateForShift(
        UUID tenantId,
        UUID branchId,
        UUID cashShiftId,
        UUID menuTemplateId,
        Map<UUID, Integer> customQuotas,
        UUID assignedBy
    ) {
        Optional<ShiftMenuAssignment> existing = shiftMenuAssignmentRepository.findByCashShiftId(cashShiftId);
        if (existing.isPresent()) {
            throw new ResourceConflictException("El turno ya cuenta con un menú asignado.");
        }

        return instantiateForServiceSession(
            tenantId,
            branchId,
            null,
            cashShiftId,
            menuTemplateId,
            customQuotas,
            List.of(),
            assignedBy
        );
    }

    private ShiftMenuDto instantiateForServiceSession(
        UUID tenantId,
        UUID branchId,
        UUID serviceSessionId,
        UUID cashShiftId,
        UUID menuTemplateId,
        Map<UUID, Integer> customQuotas,
        List<UUID> unlimitedDishIds,
        UUID assignedBy
    ) {
        DailyMenu template = dailyMenuRepository.findByIdAndTenantId(menuTemplateId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Plantilla de Menú", menuTemplateId));

        ShiftMenuAssignment assignment = new ShiftMenuAssignment(
            tenantId,
            branchId,
            cashShiftId,
            template.getId(),
            assignedBy
        );
        assignment.setServiceSessionId(serviceSessionId);

        Set<UUID> unlimitedSet = unlimitedDishIds != null ? new HashSet<>(unlimitedDishIds) : Set.of();

        for (DailyMenuItem templateItem : template.getItems()) {
            Dish dish = templateItem.getDish();
            if (!dish.isActive()) {
                continue;
            }

            boolean isUnlimited = templateItem.isUnlimited() || unlimitedSet.contains(dish.getId());
            int initialQuota = isUnlimited
                ? 0
                : ((customQuotas != null && customQuotas.containsKey(dish.getId()))
                    ? customQuotas.get(dish.getId())
                    : templateItem.getSuggestedQuota());

            BigDecimal effectivePrice = templateItem.getOverridePrice() != null
                ? templateItem.getOverridePrice()
                : dish.getSalePrice();

            ShiftMenuItem shiftItem = new ShiftMenuItem(
                assignment,
                dish,
                templateItem.getCourseType(),
                initialQuota,
                initialQuota,
                0,
                effectivePrice,
                !isUnlimited && initialQuota <= 0
            );
            shiftItem.setAllowIndividualSale(templateItem.isAllowIndividualSale());
            shiftItem.setUnlimited(isUnlimited);
            assignment.addItem(shiftItem);
        }

        ShiftMenuAssignment saved = shiftMenuAssignmentRepository.save(assignment);
        return toShiftMenuDto(saved, template);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShiftMenuDto> getShiftMenu(UUID cashShiftId) {
        Optional<ShiftMenuAssignment> direct = shiftMenuAssignmentRepository.findByCashShiftId(cashShiftId);
        if (direct.isPresent()) {
            return direct.map(this::resolveAndMapAssignment);
        }
        Optional<UUID> sessionIdOpt = resolveServiceSessionIdForShift(cashShiftId);
        if (sessionIdOpt.isPresent()) {
            return shiftMenuAssignmentRepository.findByServiceSessionId(sessionIdOpt.get())
                .map(this::resolveAndMapAssignment);
        }
        UUID tenantId = TenantContextHolder.getTenantId();
        UUID branchId = TenantContextHolder.getBranchId();
        if (tenantId != null && branchId != null) {
            return branchServiceSessionRepository.findOpenSessionByBranch(tenantId, branchId)
                .flatMap(s -> shiftMenuAssignmentRepository.findByServiceSessionId(s.getId()))
                .map(this::resolveAndMapAssignment);
        }
        return Optional.empty();
    }

    private Optional<UUID> resolveServiceSessionIdForShift(UUID cashShiftId) {
        if (jdbcClient == null || cashShiftId == null) {
            return Optional.empty();
        }
        try {
            return jdbcClient.sql("SELECT service_session_id FROM cash_shifts WHERE id = :id")
                .param("id", cashShiftId)
                .query(UUID.class)
                .optional();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private ShiftMenuDto resolveAndMapAssignment(ShiftMenuAssignment assignment) {
        DailyMenu menu = assignment.getMenuId() != null
            ? dailyMenuRepository.findById(assignment.getMenuId()).orElse(null)
            : null;
        return toShiftMenuDto(assignment, menu);
    }

    @Override
    public ShiftMenuItemDto adjustQuota(UUID shiftId, UUID itemId, int quantityDelta, String reason, UUID supervisorId) {
        ShiftMenuItem item = shiftMenuItemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Ítem de Menú de Turno", itemId));

        int rows = shiftMenuItemRepository.adjustQuota(itemId, quantityDelta, item.getVersion());
        if (rows == 0) {
            throw new ResourceConflictException("Conflicto de concurrencia al actualizar cupos en el turno.");
        }

        ShiftMenuQuotaAdjustment adjustment = new ShiftMenuQuotaAdjustment(
            item,
            supervisorId,
            quantityDelta,
            reason
        );
        shiftMenuQuotaAdjustmentRepository.save(adjustment);

        ShiftMenuItem updated = shiftMenuItemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Ítem de Menú de Turno", itemId));

        return toShiftMenuItemDto(updated);
    }

    @Override
    public void enforceAndDecrementQuota(UUID cashShiftId, UUID dishId, int quantity) {
        enforceAndDecrementQuota(null, cashShiftId, dishId, quantity);
    }

    @Override
    public void enforceAndDecrementQuota(UUID branchId, UUID cashShiftId, UUID dishId, int quantity) {
        if (dishId == null || quantity <= 0) {
            return;
        }

        Optional<ShiftMenuItem> shiftItemOpt = cashShiftId != null
            ? shiftMenuItemRepository.findByCashShiftIdAndDishId(cashShiftId, dishId)
            : Optional.empty();

        if (shiftItemOpt.isEmpty() && cashShiftId != null) {
            Optional<UUID> sessionIdOpt = resolveServiceSessionIdForShift(cashShiftId);
            if (sessionIdOpt.isPresent()) {
                shiftItemOpt = shiftMenuItemRepository.findByServiceSessionIdAndDishId(sessionIdOpt.get(), dishId);
            }
        }

        if (shiftItemOpt.isEmpty() && branchId != null) {
            UUID tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                Optional<BranchServiceSession> sessionOpt = branchServiceSessionRepository.findOpenSessionByBranch(tenantId, branchId);
                if (sessionOpt.isPresent()) {
                    shiftItemOpt = shiftMenuItemRepository.findByServiceSessionIdAndDishId(sessionOpt.get().getId(), dishId);
                }
            }
        }

        if (shiftItemOpt.isEmpty()) {
            // El plato no pertenece al menú diario del turno/jornada (carta abierta); no se aplica cupo de raciones.
            return;
        }

        ShiftMenuItem item = shiftItemOpt.get();
        if (!item.isUnlimited() && (item.getCurrentQuota() < quantity || item.isSoldOut())) {
            throw new ItemSoldOutException(
                "El plato '" + item.getDish().getName() + "' solo dispone de " +
                item.getCurrentQuota() + " raciones y se solicitaron " + quantity + "."
            );
        }

        int updatedRows = shiftMenuItemRepository.decrementQuota(item.getId(), quantity, item.getVersion());
        if (updatedRows == 0) {
            ShiftMenuItem reloaded = shiftMenuItemRepository.findById(item.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ítem de Menú", item.getId()));
            if (!reloaded.isUnlimited() && (reloaded.getCurrentQuota() < quantity || reloaded.isSoldOut())) {
                throw new ItemSoldOutException(
                    "El plato '" + reloaded.getDish().getName() + "' solo dispone de " +
                    reloaded.getCurrentQuota() + " raciones y se solicitaron " + quantity + "."
                );
            }
            throw new ResourceConflictException("Conflicto concurrente al decrementar cupo para el plato: " + reloaded.getDish().getName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShiftMenuItemDto> findShiftMenuItem(UUID cashShiftId, UUID dishId) {
        Optional<ShiftMenuItem> direct = shiftMenuItemRepository.findByCashShiftIdAndDishId(cashShiftId, dishId);
        if (direct.isPresent()) {
            return direct.map(this::toShiftMenuItemDto);
        }
        return resolveServiceSessionIdForShift(cashShiftId)
            .flatMap(sessionId -> shiftMenuItemRepository.findByServiceSessionIdAndDishId(sessionId, dishId))
            .map(this::toShiftMenuItemDto);
    }

    private DailyMenuDto toDailyMenuDto(DailyMenu menu) {
        List<DailyMenuItemDto> items = menu.getItems().stream()
            .sorted(Comparator.comparingInt(DailyMenuItem::getSortOrder))
            .map(i -> new DailyMenuItemDto(
                i.getId(),
                i.getDish().getId(),
                i.getDish().getName(),
                i.getDish().getCode(),
                i.getCourseType(),
                i.getSuggestedQuota(),
                i.getOverridePrice(),
                i.isMandatoryInPackage(),
                i.getSortOrder(),
                i.isAllowIndividualSale(),
                i.isUnlimited()
            ))
            .toList();

        List<UUID> comboIds = menu.getCombos().stream()
            .sorted(Comparator.comparingInt(DailyMenuCombo::getSortOrder))
            .map(DailyMenuCombo::getComboId)
            .toList();

        return new DailyMenuDto(
            menu.getId(),
            menu.getTenantId(),
            menu.getBranchId(),
            menu.getName(),
            menu.getDescription(),
            menu.getDayOfWeek(),
            menu.getPriceMode(),
            menu.getPackagePrice(),
            menu.isTemplate(),
            menu.isActive(),
            menu.getCreatedAt(),
            menu.getUpdatedAt(),
            menu.getVersion(),
            items,
            comboIds
        );
    }

    private ShiftMenuDto toShiftMenuDto(ShiftMenuAssignment assignment, DailyMenu menu) {
        List<ShiftMenuItemDto> items = assignment.getItems().stream()
            .map(this::toShiftMenuItemDto)
            .toList();

        List<ComboDto> combos = List.of();
        if (menu != null && !menu.getCombos().isEmpty()) {
            Map<UUID, String> dishNames = dishRepository.findByTenantId(assignment.getTenantId()).stream()
                .collect(Collectors.toMap(Dish::getId, Dish::getName, (a, b) -> a));
            combos = menu.getCombos().stream()
                .sorted(Comparator.comparingInt(DailyMenuCombo::getSortOrder))
                .map(c -> comboRepository.findByIdAndTenantId(c.getComboId(), assignment.getTenantId()).orElse(null))
                .filter(c -> c != null && c.isActive())
                .map(c -> toComboDto(c, dishNames))
                .toList();
        }

        return new ShiftMenuDto(
            assignment.getId(),
            assignment.getTenantId(),
            assignment.getBranchId(),
            assignment.getCashShiftId(),
            assignment.getMenuId(),
            menu != null ? menu.getName() : null,
            assignment.getAssignedBy(),
            assignment.getAssignedAt(),
            items,
            assignment.getServiceSessionId(),
            combos
        );
    }

    private ShiftMenuItemDto toShiftMenuItemDto(ShiftMenuItem item) {
        return new ShiftMenuItemDto(
            item.getId(),
            item.getAssignment().getId(),
            item.getDish().getId(),
            item.getDish().getName(),
            item.getDish().getCode(),
            item.getCourseType(),
            item.getInitialQuota(),
            item.getCurrentQuota(),
            item.getSoldCount(),
            item.getEffectivePrice(),
            item.isSoldOut(),
            item.getVersion(),
            item.isAllowIndividualSale(),
            item.isUnlimited()
        );
    }

    private BranchServiceSessionDto toServiceSessionDto(BranchServiceSession session) {
        ShiftMenuDto shiftMenu = shiftMenuAssignmentRepository.findByServiceSessionId(session.getId())
            .map(this::resolveAndMapAssignment)
            .orElse(null);

        String menuName = shiftMenu != null
            ? shiftMenu.menuName()
            : (session.getDailyMenuId() != null
                ? dailyMenuRepository.findById(session.getDailyMenuId()).map(DailyMenu::getName).orElse(null)
                : "Carta Completa");

        return new BranchServiceSessionDto(
            session.getId(),
            session.getTenantId(),
            session.getBranchId(),
            session.getServiceDate(),
            session.getMenuMode(),
            session.getDailyMenuId(),
            menuName,
            session.getStatus(),
            session.getConfiguredBy(),
            session.getNotes(),
            session.getCreatedAt(),
            session.getActivatedAt(),
            session.getClosedAt(),
            shiftMenu
        );
    }

    private LocalDate resolveBranchServiceDate(UUID branchId) {
        try {
            String tz = branchPublicApi.getBranchTimezone(branchId);
            return LocalDate.now(ZoneId.of(tz != null && !tz.isBlank() ? tz : "America/La_Paz"));
        } catch (Exception ignored) {
            return LocalDate.now(ZoneId.of("America/La_Paz"));
        }
    }

    private ComboDto toComboDto(Combo combo, Map<UUID, String> dishNames) {
        List<ComboItemDto> itemDtos = combo.getItems().stream().map(item -> {
            List<ComboItemOptionDto> optDtos = item.getOptions().stream().map(opt ->
                new ComboItemOptionDto(
                    opt.getId(),
                    opt.getDishId(),
                    dishNames.getOrDefault(opt.getDishId(), "Plato"),
                    opt.getPriceAdjustment(),
                    opt.isDefault()
                )
            ).toList();

            String defaultDishName = item.getDefaultDishId() != null
                ? dishNames.getOrDefault(item.getDefaultDishId(), null)
                : null;

            return new ComboItemDto(
                item.getId(),
                item.getName(),
                item.getSelectionMode(),
                item.isRequired(),
                item.getDefaultDishId(),
                defaultDishName,
                item.getQuantity(),
                item.getSortOrder(),
                optDtos,
                item.getSelectionSource(),
                item.getCourseType()
            );
        }).toList();

        return new ComboDto(
            combo.getId(),
            combo.getTenantId(),
            combo.getBranchId(),
            combo.getCategoryId(),
            combo.getCode(),
            combo.getName(),
            combo.getDescription(),
            combo.getBasePrice(),
            combo.isActive(),
            itemDtos
        );
    }
}
