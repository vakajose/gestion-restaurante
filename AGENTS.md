# AGENTS.md - Protocolo Operativo para Antigravity

## 1. Alcance y Límites Técnicos
- **Entorno Host / OS:** WSL2 sobre Linux Fedora (gestor de paquetes `dnf`). Cliente `postgresql` (`psql`) instalado localmente para diagnósticos de base de datos.
- **Backend:** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith, Maven, PostgreSQL, Flyway.
- **Frontend:** Angular 22 (Zoneless, Signal Forms, Standalone), Tailwind CSS (Plantilla TailwindAdmin), Dexie.js (IndexedDB).
- **Modelo de Aislamiento:** Toda entidad transaccional exige `tenant_id` (UUID) y `branch_id` (UUID). El contexto se obtiene de `TenantContextHolder` (propagado por JWT).

## 2. Reglas Arquitectónicas Inviolables
1. **Validación de Límites Modulares:** La compilación debe validar `ApplicationModules.of(RestaurantApplication.class).verify()`. No se permiten dependencias circulares entre paquetes ni inyecciones de repositorios de otros módulos.
2. **Comunicación Asíncrona / Eventos:**
   - La venta en POS publica `OrderPaidEvent`.
   - El Kardex consume el evento mediante `@ApplicationModuleListener` para aplicar deducciones de recetas atómicamente.
3. **Manejo Offline First:** Las mutaciones en POS deben persistirse en Dexie.js localmente antes de invocar la API mediante `client_transaction_id` idempotente.
4. **Manejo Estricto de Fechas y Zonas Horarias (ADR-0006):**
   - **Fechas de calendario puras (sin hora):** Usar `DATE` en PostgreSQL, `java.time.LocalDate` en Java y string ISO `"YYYY-MM-DD"` en JSON/TypeScript. Prohibido agregar hora o zona a campos `DATE`. Formateo UI: `dd/MM/yyyy`.
   - **Marcas de tiempo transaccionales:** Usar `TIMESTAMPTZ` en PostgreSQL, `java.time.Instant` en Java y string ISO UTC con sufijo `Z` en JSON.
   - **Hora Local de Sucursal:** Los cortes de turno y consultas de ventas del día se calculan convirtiendo el timestamp a la zona horaria de la sucursal (`branches.timezone`, default `'America/La_Paz'`). La UI siempre muestra la hora en el huso local de la sucursal.

## 3. Protocolo de Trabajo en Git
- Cada tarea de `docs/tasks.md` se realiza en su propia rama: `git checkout -b feat/<modulo>-<tarea>`.
- Commits siguiendo Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`).
- Antes de entregar la rama:
  - `./mvnw clean test` (Backend: 100% de tests pasando sin warnings).
  - `npm run lint && npm test` (Frontend: validación estricta de tipos y linting).

## 4. Guía de Interacción y Comandos Antigravity
- `/plan`: Utilizar obligatoriamente antes de iniciar la implementación de cualquier tarea compleja para diseñar y acordar el plan técnico.
- `/grill-me`: Recomendar cuando se requiera entrevistar al usuario para destrabar reglas de negocio específicas (ej. arqueo, recetas, promociones).
- `/boost`: Recomendar cuando se aborden problemas de alta complejidad (concurrencia, optimización de consultas Kardex, sincronización Dexie).
- `/learn`: Usar para registrar y persistir preferencias de diseño o patrones corregidos por el usuario.