---
title: "ADR 0004: Catálogo Híbrido: Sobreescrituras y Clonación Estructural"
category: "adr"
tags:
  - adr
  - architecture
  - catalog
  - multi-branch
last_updated: "2026-11-17"
status: "accepted"
---

# ADR 0004: Catálogo Híbrido: Sobreescrituras y Clonación Estructural

## Contexto
Las empresas con múltiples sucursales a menudo requieren:
1. Ofrecer el mismo catálogo base en todas sus sucursales, pero activando o desactivando ciertos platos según la localidad.
2. Manejar precios diferenciados por sucursal (ejemplo: precios más altos en sucursales de centros comerciales o aeropuertos).
3. Modificar estructuralmente la receta de un plato en una sucursal específica (por ejemplo, omitir un ingrediente no disponible regionalmente o modificar el gramaje de ración).

Si se intenta resolver la modificación estructural mediante tablas complejas de sobreescritura de recetas por sucursal, se introduce una sobreingeniería excesiva con riesgo de inconsistencias graves al calcular costos en Kardex.

## Decisión
Se implementa una estrategia dual simple, justa y suficiente:
1. **Sobreescritura Liviana (`branch_dishes`):**
   - Para platos maestros existentes en `dishes`, la sucursal puede activar/desactivar (`is_available`) y definir un precio alternativo (`price_override`). La receta original (BOM) se mantiene idéntica.
2. **Clonación Estructural (`dishes.cloned_from_id` y `branch_id`):**
   - Si la sucursal necesita cambiar los ingredientes o la composición del plato, el sistema permite **clonar** el plato maestro. El nuevo plato se crea asignado a esa sucursal específica (`branch_id = sucursal`), manteniendo la trazabilidad con `cloned_from_id`.
   - La nueva receta (`dish_recipes`) se modifica libremente sin afectar a otras sucursales ni requerir lógica condicional compleja en el cálculo de costos.

## Consecuencias
- **Positivas:** Cero sobreingeniería; claridad total en el cálculo de recetas y deducciones de stock; control granular por sucursal respetando el principio KISS.
- **Negativas:** La interfaz administrativa debe proveer una acción intuitiva de "Clonar para esta sucursal".

## Enmienda y Evolución (ADR-0030)
> [!NOTE]
> Complementado por [ADR-0030](./0030_catalog_branch_isolation_by_default.md) (`TASK-632`, `TASK-633`): a partir de Sprint 6, el parámetro `catalog.scope_mode` adopta **`BRANCH_ISOLATED` por defecto**, asignando los platos y combos nuevos directamente a la sucursal activa (`branch_id`) salvo que la empresa configure explícitamente `GLOBAL_SHARED`. Se mantiene la compatibilidad de overrides `branch_dishes` y la clonación/duplicación explícita vía `POST /api/v1/catalog/dishes/{dishId}/clone` (`CloneDishRequest`).

---

## Documentos Relacionados
- [ADR-0022: Asistente Secuencial SKU, Selector Emoji y Ordenamiento](./0022_sequential_sku_assistant_category_emoji_picker_and_ordering.md)
- [ADR-0030: Aislamiento del Catálogo por Sucursal por Defecto](./0030_catalog_branch_isolation_by_default.md)
- [ADR-0031: Sesiones de Servicio por Sucursal y Combos Dinámicos](./0031_branch_service_sessions_and_dynamic_combos.md)
- [Casos de Uso de Catálogo](../project/architecture/03-use-cases/use_cases_catalog.md)
- [Diagrama de Entidad-Relación Físico](../project/architecture/02-domain-and-data/database_erd.md)

