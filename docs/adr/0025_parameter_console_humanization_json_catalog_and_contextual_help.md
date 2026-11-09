# ADR-0025: Humanización de Consola de Parámetros, Catálogo JSON Desacoplado y Tooltips Contextuales (PB-047)

- **Estado:** `ACEPTADO`
- **Fecha:** 2026-11-09
- **Autores:** Squad Multi-Agente & Líder de Proyecto (Co-Diseño Interactivo)
- **Alcance:** Configuración & Parámetros (`modules.configuration`), Consola de Parámetros (`/settings/parameters`), Catálogo de Metadatos JSON, UX/UI Gastronómica
- **Requerimiento:** `PB-047` (Humanización Visual, Etiquetas Descriptivas y Rediseño de la Consola de Parametría Multinivel)
- **Tareas Relacionadas:** `TASK-510` [CONFIG] [UI/UX]

---

## 1. Contexto y Problemática

En [ADR-0008](./0008_unified_multilevel_parameter_system.md) se formalizó el sistema unificado de parametrización multinivel con catálogo en código y almacenamiento disperso (*sparse storage*). Aunque este modelo resolvió eficientemente el almacenamiento y la resolución jerárquica ($O(1)$) en base de datos, la experiencia de usuario y la arquitectura de metadatos presentaban deficiencias críticas detectadas en la retrospectiva del Sprint 3 (`OBS-CFG-01`):

1. **Textos y Metadatos Acoplados en Código Java ("Quemados"):** Las definiciones de nombres, descripciones y opciones estaban incrustadas directamente en records Java dentro de `SystemParameterCatalog.java`. Modificar una tilde, corregir una descripción o añadir textos explicativos obligaba a modificar código Java y recompilar el monolito.
2. **Jerarquía Visual Invertida en la Consola (`/settings/parameters`):** La clave canónica técnica (ej. `security.session_inactivity_timeout_minutes`) competía visualmente con el nombre descriptivo, confundiendo a administradores y encargados de restaurante sin perfil técnico.
3. **Opciones Desplegables Crudas en Inglés (Enums):** Los controles `<select>` renderizaban los valores enums directos del backend (ej. `MANUAL_ISOLATED`, `AUTOMATIC_PERMISSIVE`, `ALLOW_AUTO_BUFFER_REUSE`, `SINGLE_PER_BRANCH`), sin traducción gastronómica ni explicación del impacto en cocina o caja.
4. **Ausencia de Ayuda Contextual con Casos Reales:** La descripción técnica de una sola línea era insuficiente para tomar decisiones operativas sobre parámetros de alta criticidad (ej. deducciones de inventario o políticas de merma).
5. **Búsqueda Limitada:** El buscador de parámetros solo filtraba por clave exacta o título, impidiendo localizar configuraciones mediante términos cotidianos del restaurante (ej. *"mesas"*, *"kardex"*, *"mozo"*, *"repartidor"* o *"merma"*).

---

## 2. Decisiones Arquitectónicas

### A. Desacoplamiento de Metadatos en Catálogo JSON (`parameters-catalog.json`)
Siguiendo las mejores prácticas de la industria (estándares de *VS Code Configuration Schema* y *Kubernetes CRDs*):
- Se extraen todos los textos descriptivos, etiquetas humanizadas, ejemplos prácticos y opciones de negocio a un archivo estructurado:
  `backend/src/main/resources/config/parameters-catalog.json`.
- **Estructura del Esquema JSON:**
  ```json
  [
    {
      "key": "inventory.deduction_mode",
      "name": "Modo de Deducción de Inventario",
      "description": "Define la política de descuento de existencias en Kardex al procesar órdenes.",
      "category": "INVENTORY",
      "type": "ENUM",
      "defaultValue": "AUTOMATIC_PERMISSIVE",
      "allowedScope": "TENANT",
      "criticality": "HIGH",
      "requiresRestart": false,
      "help": {
        "impact": "Determina si el sistema permite cobrar ventas cuando no hay existencias suficientes o si bloquea la comanda.",
        "example": "Si vendes una Hamburguesa y solo queda pan para una, el modo estricto impedirá añadir una segunda unidad al pedido."
      },
      "options": [
        {
          "value": "AUTOMATIC_PERMISSIVE",
          "label": "Automático permisivo",
          "description": "Descuenta stock en Kardex y alerta visualmente si queda en saldo negativo."
        },
        {
          "value": "AUTOMATIC_STRICT",
          "label": "Automático estricto",
          "description": "Bloquea la comanda si falta algún ingrediente de la receta en inventario."
        },
        {
          "value": "MANUAL_ISOLATED",
          "label": "Aislado / Manual",
          "description": "No descuenta insumos al vender; el Kardex se ajusta únicamente mediante arqueo físico."
        }
      ]
    }
  ]
  ```
- **Carga en Backend (`SystemParameterCatalog.java`):**
  - Al inicializar el contexto de Spring Boot, Jackson parsea `parameters-catalog.json` en memoria ($< 3\text{ ms}$).
  - El enum `SystemParameter.java` mantiene la tipificación estricta en código Java para prevenir errores de compilación y respaldar las llamadas de los módulos consumidores.

### B. Extensión de Contratos DTO en la API Pública (`modules.configuration.api`)
El DTO `SystemParameterDto` se enriquece para entregar la metadata completa al frontend en una única llamada:
```java
public record SystemParameterDto(
    String key,
    String name,
    String description,
    ParameterCategory category,
    ParameterType type,
    String effectiveValue,
    String defaultValue,
    ParameterScope scope,
    ParameterScope inheritedFrom,
    boolean isOverridden,
    ParameterCriticality criticality,
    boolean requiresRestart,
    List<ParameterOptionDto> options,
    ParameterHelpDto help,
    Instant updatedAt,
    UUID updatedBy,
    String auditReason
) {}

public record ParameterOptionDto(
    String value,
    String label,
    String description
) {}

public record ParameterHelpDto(
    String impact,
    String example
) {}
```

### C. Rediseño Visual de Tarjetas de Parámetros (`parameters.component.html`)
1. **Encabezado Ejecutivo:**
   - Nombre en lenguaje natural como elemento primario: `text-base font-semibold text-link dark:text-white`.
   - Clave canónica técnica en badge monospace tenue (`text-[10px] font-mono bg-lightgray/60 text-bodytext px-2 py-0.5 rounded border border-border/50`), acompañado de un botón interactivo de un clic para copiar la clave al portapapeles (`navigator.clipboard.writeText`).
   - Insignias suaves (`<app-badge>` soft) para ámbito (`Por Sucursal`, `Empresa Matriz`) y criticidad (`Estándar`, `Media`, `Alta - Requiere Confirmación`).
2. **Controles Humanizados por Tipo de Dato:**
   - **`ENUM`:** Elemento `<select>` nativo estilizado con TailwindAdmin. Renderiza el texto humanizado en `<option>` (`{{ opt.label }}`) con su descripción auxiliar, manteniendo como valor interno el código del enum.
   - **`BOOLEAN`:** Switch toggle reactivo complementado con una etiqueta de texto dinámica a su lado (`"Activado"` en verde / `"Desactivado"` en gris neutro) para máxima accesibilidad y claridad.
   - **`INTEGER` / `DOUBLE` / `STRING`:** Inputs en línea con etiquetas de sufijo/unidad según el contexto (ej. `"minutos"`, `"BOB"`, `"%"`).

### D. Componente Standalone de Ayuda Contextual (`<app-context-help>`)
En estricto cumplimiento de la Regla 9 de `AGENTS.md` (evitar librerías pesadas como *Tippy.js* o *Popper*), se implementa un componente nativo ultraliviano ($< 2\text{ KB}$):
- Botón accesible `(?)` circular junto al título del parámetro.
- Popover flotante con backdrop o auto-cierre en clic exterior (`@HostListener('document:click')`) y tecla `Escape`.
- Renderiza dos bloques destacados:
  - **Impacto Operativo:** Explicación de cómo altera el flujo de trabajo en sala, cocina o caja.
  - **Ejemplo en Restaurante:** Escenario de la vida real gastronómica.

### E. Buscador Predictivo Multicriterio en Tiempo Real
El buscador de la consola (`searchInput`) con debounce de 250ms filtra evaluando simultáneamente:
- Nombre descriptivo (`param.name`) y clave técnica (`param.key`).
- Descripción del parámetro (`param.description`).
- Etiquetas y descripciones de las opciones (`option.label`, `option.description`).
- Textos de impacto y ejemplo práctico (`help.impact`, `help.example`).
Permite localizar parámetros mediante lenguaje coloquial de restaurante (ej. buscar *"cancelar hamburguesa"* encuentra `pos.cancellation_prepared_waste_policy`).

---

## 3. Consecuencias y Beneficios

### Positivas
- **Mantenibilidad Óptima:** Textos, explicaciones y ejemplos gastronómicos se editan en un único archivo JSON sin tocar código Java ni alterar esquemas relacionales.
- **Frontend 100% Dinámico:** El componente Angular es un renderizador agnóstico; cualquier nuevo parámetro configurado en el catálogo backend aparece instantáneamente en la interfaz con sus opciones y ayudas.
- **Cero Dependencias Adicionales:** Popover nativo en Angular y parseo con Jackson ya existente en Spring Boot.
- **Comprensión Total para Operadores:** Desaparecen los enums crudos en inglés; el personal administrativo y operativo comprende exactamente qué activa cada parámetro antes de guardar.

### Negativas / Mitigaciones
- **Validación del JSON:** Si el archivo JSON tiene errores de sintaxis, el arranque de la aplicación podría fallar.
  - *Mitigación:* Se implementa una prueba unitaria JUnit `@Test void validateParametersCatalogJson()` que valida el parseo de todas las entradas del catálogo al compilar (`mvn test`).
