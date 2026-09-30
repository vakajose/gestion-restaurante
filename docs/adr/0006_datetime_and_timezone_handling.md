# ADR 0006: Estándar para Manejo de Fechas, Horas y Zonas Horarias

## Contexto
En un sistema de restaurante (POS y ERP), los errores en el manejo de fechas provocan discrepancias graves:
1. **Desfase de +/- 1 día en fechas de calendario:** Si un gasto o compra se almacena como timestamp UTC (`2026-09-30 00:00:00 UTC`), al deserializarse en una zona horaria como Bolivia (`America/La_Paz`, UTC-4), se convierte en `2026-09-29 20:00:00`, cambiando el día contable del gasto.
2. **Cierre de Turnos y Ventas del Día:** Si el restaurante cierra su turno a las 23:30 hora local, en UTC ya es el día siguiente (03:30 UTC). Si las consultas agrupan por `DATE(created_at)` en UTC, las ventas nocturnas quedan asignadas al día posterior, falseando los reportes de utilidad diaria.

## Decisión
Se establece una política arquitectónica estricta para la gestión de fechas y horas en todo el proyecto:

### 1. Fechas de Calendario Puras (Date-only, sin hora)
Aplica a campos que representan un día específico en el calendario contable: `expenses.expense_date`, fechas de facturas de compras, fechas de nacimiento o filtros de rangos de fechas (`startDate`, `endDate`).
- **Base de Datos (PostgreSQL):** Tipo `DATE`.
- **Backend (Java):** `java.time.LocalDate`.
- **Contrato JSON (REST):** Formato ISO-8601 estricto `"YYYY-MM-DD"` (ej. `"2026-09-30"`). **Queda estrictamente prohibido incluir componentes de tiempo (`T00:00:00`) o zona horaria**.
- **Frontend (Angular):** Tratado como `string` en formato ISO (`YYYY-MM-DD`). Para inputs: `<input type="date">`. Formateo al usuario: `dd/MM/yyyy` (ej. `30/09/2026`).

### 2. Marcas de Tiempo Transaccionales (Timestamps precisos)
Aplica a eventos del sistema: `created_at`, `updated_at`, `orders.closed_at`, `cash_shifts.opened_at`, `cash_shifts.closed_at`, `kardex_movements.movement_date`, `audit_logs.created_at`.
- **Base de Datos (PostgreSQL):** Tipo `TIMESTAMP WITH TIME ZONE` (`TIMESTAMPTZ`). Almacena el instante universal UTC.
- **Backend (Java):** `java.time.Instant`.
- **Contrato JSON (REST):** Formato ISO-8601 en UTC con sufijo `Z` (ej. `"2026-09-30T17:28:34.123Z"`).
  - Configuración Spring Boot Jackson:
    ```properties
    spring.jackson.serialization.write-dates-as-timestamps=false
    spring.jackson.time-zone=UTC
    ```

### 3. Zona Horaria Local por Sucursal (`branches.timezone`)
- Cada sucursal física almacena su zona horaria IANA en `branches.timezone` (valor por defecto: `'America/La_Paz'`).
- Las consultas SQL que requieren agrupar por el día local de la sucursal aplican la conversión de zona:
  ```sql
  WHERE (orders.created_at AT TIME ZONE branches.timezone)::date = :targetDate
  ```
- **Frontend (Angular):** La presentación visual de timestamps siempre convierte el instante UTC a la hora local de la sucursal activa mediante un pipe o utilidad centralizada (`DatePipe` con timezone de sucursal):
  - Formato estándar de visualización: `dd/MM/yyyy HH:mm:ss` (ej. `30/09/2026 13:28:34`).

## Consecuencias
- **Positivas:** Cero desajustes de fechas entre frontend y backend; reportes financieros exactos acordes al huso horario del restaurante; soporte transparente para expansión a otras regiones horarias.
- **Negativas:** Exige rigurosidad en los desarrolladores para no usar `LocalDateTime` en campos que deben ser `LocalDate` o `Instant`.
