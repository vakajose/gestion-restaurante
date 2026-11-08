# ADR-0022: Asistente de SKU Secuencial (code_prefix), Selector de Emojis Gastronómicos y Ordenamiento en Catálogo (PB-048)

- **Estado:** `ACEPTADO`
- **Fecha:** 2026-11-08
- **Autores:** Squad Multi-Agente & Líder de Proyecto (Co-Diseño Interactivo / Grill-Me)
- **Alcance:** Catálogo de Platos y Categorías, UI/UX de Administración, Generación Asistida de SKUs, Emojis Gastronómicos
- **Requerimiento:** `PB-048` (Asistente de Codificación Secuencial de Platos y Selector de Iconos/Emojis para Categorías)
- **Tareas Relacionadas:** `TASK-507` [CATALOG] [UI/UX]

---

## 1. Contexto y Problemática

En la administración de cartas y menús gastronómicos se identificaron tres fuentes recurrentes de fricción y errores de usuario:
1. **Fricción en la Codificación SKU:** El alta de platos requería que el usuario inventara manualmente un código alfanumérico (ej. `PL01`, `HAM-01`). Si el código colisionaba con otro plato existente, el backend arrojaba un error de conflicto (`409 Conflict`), entorpeciendo el alta masiva de productos.
2. **Iconografía Manual y Poco Intuitiva:** El campo de icono para categorías era un input de texto libre con placeholder *"☕"*. El administrador debía abrir el teclado del sistema operativo o buscar y pegar emojis desde sitios web externos.
3. **Colisión de Orden Visual:** El ordenamiento de categorías (`sort_order`) se dejaba a criterio numérico manual, provocando que múltiples categorías quedaran empatadas con orden `0` o desordenadas en el POS.
4. **Preservación de Trazabilidad Histórica:** La codificación de un plato no puede ser efímera; debe permanecer inmutable ante cambios de nombres o prefijos de categorías para proteger la integridad de comandas y comprobantes contables históricos.

---

## 2. Decisiones Arquitectónicas

### A. Modelo Relacional: Prefijo de Categoría (`code_prefix`)
- Se añade la columna `code_prefix VARCHAR(10) NOT NULL` a la tabla `categories` (migración Flyway `V15__category_code_prefix.sql`).
- **Restricción de Unicidad:** `CONSTRAINT uq_categories_tenant_prefix UNIQUE (tenant_id, code_prefix)`. Dos categorías no pueden compartir el mismo prefijo en un tenant para evitar ambigüedades.
- **Sugerencia Inicial:** Al crear una categoría, el frontend sugiere automáticamente un prefijo derivando las primeras 3 letras mayúsculas del nombre sin acentos ni espacios (ej. "Bebidas" $\rightarrow$ `BEB`, "Hamburguesas" $\rightarrow$ `HAM`, "Pizzas" $\rightarrow$ `PIZ`). El usuario puede modificarlo libremente antes de guardar.
- **Migración Histórica:** Para las categorías ya existentes, la migración `V15` genera automáticamente su prefijo a partir de las primeras 3 letras alfanuméricas de su nombre actual, garantizando la restricción `NOT NULL`.

### B. Generación Asistida de SKU en Backend (`MAX + 1` sin Ceros a la Izquierda)
- **Endpoint REST:** `GET /api/v1/catalog/dishes/next-code?categoryId={id}`
- **Algoritmo Secuencial:**
  1. Obtiene el `code_prefix` de la categoría indicada.
  2. Consulta en la tabla `dishes` de la sucursal/tenant todos los códigos que sigan el patrón `${prefix}-%`:
     `SELECT code FROM dishes WHERE tenant_id = :tenantId AND branch_id IS NOT DISTINCT FROM :branchId AND code LIKE :prefixPattern`
  3. Extrae la porción numérica posterior al guion y calcula:
     $$\text{siguiente} = \max(\text{consecutivos existentes}) + 1$$
     Si no existen platos registrados con ese prefijo, inicia en `1`.
  4. Formato estándar: **`${prefix}-${siguiente}`** en formato simple sin ceros a la izquierda (ej. `BEB-1`, `BEB-2`, `HAM-15`).
- **Invariante de Auditoría:** No se rellenan huecos de platos eliminados/inactivos; el correlativo siempre incrementa sobre el máximo histórico (`MAX + 1`).
- **Inmutabilidad de Platos Existentes:** Si el administrador edita una categoría y cambia su prefijo (ej. de `BEB` a `DRK`), los platos creados con anterioridad conservan intacto su código original (`BEB-1`). El nuevo prefijo solo regirá para las nuevas creaciones.

### C. UX del Campo de Código en Alta de Platos
- Al seleccionar una categoría en el modal de nuevo plato, se consulta el endpoint `next-code` y se precarga el código sugerido en el input `newDishCode`.
- **Respeto a Códigos Propios:** Si el usuario ingresa manualmente su propio código o escanea un código de barras (EAN-13), el sistema no lo sobreescribe automáticamente al alterar otros campos.
- Botón de acción rápida: `[🪄 Sugerir Código]` junto al input para regenerar la sugerencia bajo demanda.
- En caso de falta de conectividad, se bloquea amigablemente el alta de platos hasta restablecer la red (operación de configuración de backoffice).

### D. Selector de Emojis Nativo Popover (`app-emoji-picker`)
Aplicando la **Regla 9/10 de Gobernanza Tecnológica**, se descartan librerías npm universales pesadas (150-350 KB) en favor de un componente standalone nativo ligero ($< 2\text{ KB}$):
- **Formato:** Popover flotante que se despliega al hacer clic en el botón del icono actual en el formulario modal de categoría.
- **Catálogo Curado (~40 Emojis Gastronómicos):**
  - 🍔 **Comida Rápida:** 🍔, 🍟, 🍕, 🌭, 🥪, 🌮, 🌯
  - 🥩 **Carnes y Platos:** 🥩, 🍗, 🍖, 🥘, 🍝, 🍲, 🍤, 🍣, 🍚
  - 🥗 **Saludable y Entradas:** 🥗, 🥑, 🥟, 🧀, 🥐, 🥖
  - 🍰 **Postres y Dulces:** 🍰, 🍦, 🍩, 🍪, 🍫, 🥞
  - 🍹 **Bebidas y Cafetería:** ☕, 🧃, 🥤, 🍺, 🍷, 🍸, 🧋
  - 🍽️ **Utensilios / Menaje:** 🍽️, 🥢, 🥣, 🍳, 🧂
- **Búsqueda / Texto Libre:** Input integrado para filtrar la grilla rápida o pegar cualquier emoji externo no listado.

### E. Autocálculo del Orden Visual (`sort_order`)
- Al abrir el modal de nueva categoría:
  $$\text{sortOrder} = \max(\text{sortOrders de categorías activas}) + 1$$
- Si no hay categorías registradas, inicia en `1`. El campo permanece editable pero se precarga al final de la lista, evitando colisiones en `0`.

### F. Integración Visual con POS y Tarjetas Fallback
- Las pestañas de categorías en el POS (`PosComponent`) renderizan el icono junto al nombre (`[ ☕ Bebidas Calientes ]`, `[ 🍔 Hamburguesas ]`).
- En la cuadrícula de platos con modo galería (`TASK-506`), si un plato carece de foto, la tarjeta renderiza el emoji de su categoría como icono central sobre el degradado de fondo.

### G. Principio de Eliminación Lógica y Trazabilidad (PB-056)
- Se asienta el principio arquitectónico de que los registros de negocio no deben eliminarse físicamente de la base de datos para no quebrar la trazabilidad de tickets históricos. Se cataloga formalmente **`PB-056`** en el Product Backlog (P3) para la adopción sistemática de Soft Deletes en iteraciones posteriores.

---

## 3. Consecuencias y Beneficios

- **Cero Fricción en Carga de Catálogo:** El administrador crea platos y categorías con sugerencias inteligentes inmediatas en 1 clic.
- **Determinismo Contable:** Códigos correlativos limpios (`BEB-1`, `HAM-2`), inmutables y garantizados como únicos.
- **Experiencia de Usuario Atractiva y Ligera:** Selector de emojis temático de $< 2\text{ KB}$ sin bloatware ni dependencias externas.
- **Consistencia Visual:** Categorías y tarjetas del POS integradas cromática e iconográficamente.
