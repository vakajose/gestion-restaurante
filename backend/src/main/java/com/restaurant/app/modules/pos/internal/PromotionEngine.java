package com.restaurant.app.modules.pos.internal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Component
public class PromotionEngine {

    public static final String DEFAULT_TIMEZONE = "America/La_Paz";

    /**
     * Evalúa si una promoción está vigente temporalmente en una sucursal en un instante dado (ADR-0006).
     */
    public boolean isPromotionActiveNow(Promotion promo, Instant instant, String branchTimezone, UUID branchId) {
        if (!promo.isActive()) {
            return false;
        }

        // 1. Filtrado por sucursal asignada (si la promoción especifica sucursales)
        if (branchId != null && promo.getBranches() != null && !promo.getBranches().isEmpty()) {
            boolean branchMatches = promo.getBranches().stream()
                    .anyMatch(b -> b.getBranchId().equals(branchId));
            if (!branchMatches) {
                return false;
            }
        }

        // 2. Conversión estricta a la hora local de la sucursal (ADR-0006)
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(branchTimezone != null && !branchTimezone.isBlank() ? branchTimezone : DEFAULT_TIMEZONE);
        } catch (Exception e) {
            zoneId = ZoneId.of(DEFAULT_TIMEZONE);
        }

        ZonedDateTime localZdt = (instant != null ? instant : Instant.now()).atZone(zoneId);
        LocalDate localDate = localZdt.toLocalDate();
        LocalTime localTime = localZdt.toLocalTime();
        DayOfWeek dayOfWeek = localZdt.getDayOfWeek();

        // 3. Rango de fechas de calendario (DATE)
        if (promo.getStartDate() != null && localDate.isBefore(promo.getStartDate())) {
            return false;
        }
        if (promo.getEndDate() != null && localDate.isAfter(promo.getEndDate())) {
            return false;
        }

        // 4. Días de la semana permitidos (ej. MONDAY,TUESDAY o 1,2,3)
        if (promo.getDaysOfWeek() != null && !promo.getDaysOfWeek().isBlank()) {
            String daysStr = promo.getDaysOfWeek().trim().toUpperCase();
            String[] tokens = daysStr.split("[,;\\s]+");
            boolean dayMatches = false;
            for (String token : tokens) {
                if (token.equalsIgnoreCase(dayOfWeek.name()) ||
                    token.equals(String.valueOf(dayOfWeek.getValue()))) {
                    dayMatches = true;
                    break;
                }
            }
            if (!dayMatches) {
                return false;
            }
        }

        // 5. Ventana horaria local (TIME)
        if (promo.getStartTime() != null && promo.getEndTime() != null) {
            LocalTime start = promo.getStartTime();
            LocalTime end = promo.getEndTime();
            if (!start.isAfter(end)) {
                // Ventana normal de horario diurno (ej. 17:00:00 a 20:00:00)
                if (localTime.isBefore(start) || localTime.isAfter(end)) {
                    return false;
                }
            } else {
                // Ventana de horario nocturno cruzando medianoche (ej. 22:00:00 a 02:00:00)
                if (localTime.isBefore(start) && localTime.isAfter(end)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calcula el descuento aplicable a un ítem individual según las reglas y alcance de la promoción.
     */
    public BigDecimal calculateItemDiscount(Promotion promo, UUID itemDishId, UUID itemCategoryId, BigDecimal itemSubtotal) {
        if (itemSubtotal == null || itemSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Validación de Alcance
        String scope = promo.getScope();
        if ("DISH".equalsIgnoreCase(scope)) {
            if (promo.getTargetDishId() == null || !promo.getTargetDishId().equals(itemDishId)) {
                return BigDecimal.ZERO;
            }
        } else if ("CATEGORY".equalsIgnoreCase(scope)) {
            if (promo.getTargetCategoryId() == null || !promo.getTargetCategoryId().equals(itemCategoryId)) {
                return BigDecimal.ZERO;
            }
        }

        // Cálculo del monto de descuento
        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
            BigDecimal rate = promo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = itemSubtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(promo.getDiscountType())) {
            discount = promo.getDiscountValue();
        }

        // El descuento de un ítem no puede exceder su subtotal
        if (discount.compareTo(itemSubtotal) > 0) {
            discount = itemSubtotal;
        }
        return discount;
    }
}
