---
title: "ADR 0009: Onboarding Guiado de Empresa, Sucursal Matriz y Presets Operativos"
category: "adr"
tags:
  - adr
  - architecture
  - onboarding
  - multi-tenant
  - ux
  - presets
last_updated: "2026-11-17"
status: "accepted"
---

# ADR 0009: Onboarding Guiado de Empresa, Sucursal Matriz y Presets Operativos

## Contexto
El sistema POS + ERP requiere un flujo de incorporación autónomo (*Self-Service Onboarding*) para permitir que pequeños y medianos restaurantes se registren y comiencen a operar de inmediato sin intervención técnica ni procesos manuales de base de datos.

En el mercado objetivo de micro y pequeños negocios gastronómicos:
1. **Diversidad Fiscal:** Muchos establecimientos pequeños operan inicialmente sin NIT o en régimen simplificado; obligar el ingreso de NIT bloquea su registro. No obstante, si se introduce un NIT, debe ser estrictamente único a nivel global de plataforma para evitar suplantaciones o registros duplicados.
2. **Identificación de Acceso:** Para los restaurantes sin NIT, el identificador único de acceso es su `tenant_slug` (ej. `pizzeria-napoli`), el cual debe inferirse automáticamente del nombre comercial y ser editable de forma intuitiva.
3. **Complejidad de Configuración Inicial:** Un restaurante nuevo no debe ser forzado a cargar decenas de configuraciones, inventarios ni recetas complejas desde cero para probar el sistema.
4. **Localización y Huso Horario:** En la fase actual, el mercado se concentra en Bolivia; forzar al usuario a seleccionar zonas horarias técnicas degrada la experiencia.

---

## Decisión

Se establece el estándar arquitectónico para el servicio y experiencia de **Alta de Empresa y Onboarding** (`PB-001` / `TASK-333`):

### 1. Modelo de Identidad Fiscal y Slug Inteligente
- **NIT / Tax ID Opcional con Unicidad Estricta:**  
  La columna `tenants.nit_or_tax_id` pasa a ser `NULLABLE` en base de datos, respaldada por un índice único parcial en PostgreSQL:
  ```sql
  CREATE UNIQUE INDEX uq_tenant_nit ON tenants(nit_or_tax_id) 
  WHERE nit_or_tax_id IS NOT NULL AND nit_or_tax_id != '';
  ```
- **Login Cualificado Flexible:**  
  Los usuarios pueden identificarse en la pantalla de acceso con su contraseña y cualquiera de sus identificadores válidos: `tenant_slug` (disponible para el 100% de empresas) o `nit_or_tax_id` (si fue registrado).
- **Inferencia Automática de Slug:**  
  El slug se deriva en tiempo real en el frontend a partir del nombre comercial (`slugify`). La interfaz muestra una ayuda informativa y permite personalizarlo, validando su disponibilidad asíncronamente con debounce (`GET /api/v1/auth/onboarding/check-availability?slug=...&nit=...`; el backend también expone `GET /api/v1/auth/check-slug?slug=...`).

### 2. Estructura Real del Asistente (Wizard de 3 Pasos)
El componente frontend (`OnboardingComponent`) organiza la captura en exactamente **3 pasos**:
1. **Paso 1 — Empresa y Administrador:** Razón social (`legalName`), `slug` único, NIT opcional (`nitOrTaxId`), nombre completo del propietario (`fullName`), `username`, `email` (opcional) y `password` con validación `confirmPassword`. *(El alta inicial prescinde de carga de logotipo para acelerar el registro; el logo puede gestionarse posteriormente desde el perfil de empresa).*
2. **Paso 2 — Sucursal Matriz y Ubicación:** Nombre de la sucursal matriz (prellenado *"Sucursal Principal"*), dirección, teléfono opcional, moneda (`BOB` por defecto), huso horario (`America/La_Paz` según [ADR-0006](./0006_datetime_and_timezone_handling.md)) y **6 presets de ciudades bolivianas** (`La Paz`, `Santa Cruz`, `Cochabamba`, `El Alto`, `Tarija`, `Sucre`) que autocompletan `latitude`/`longitude` sin dependencias de mapas externos (Leaflet/OpenStreetMap).
3. **Paso 3 — Plantilla de Negocio y Menú Semilla:** Selección entre cuatro plantillas (`FAST_FOOD`, `TRADITIONAL`, `CAFE`, `EMPTY`) y casilla `includeDemoCatalog` (desactivada en `EMPTY`), seguida de un modal de resumen previo antes de ejecutar `POST /api/v1/auth/onboarding`.

### 3. Entrada "Ready to Sell" y Tour Guiado
- Tras confirmar, el backend ejecuta una transacción atómica `@Transactional` (Tenant + Branch + Rol/Usuario Admin + Presets semilla + Platos/insumos demo cuando `includeDemoCatalog = true`) y emite tokens de sesión.
- El frontend redirige directamente a `/pos` e inicializa el tour guiado interactivo (`OnboardingTourService`).

---

## Consecuencias

### Positivas
- **Cero Fricción para PyMEs:** Negocios informales o pequeños se registran en menos de 2 minutos sin exigirles NIT ni correo corporativo obligatorio.
- **Sinergia con Sparse Storage (ADR-0008):** La nueva empresa no satura la base de datos con tablas de configuración; adopta presets en $O(1)$.
- **Prueba Inmediata del POS:** Los 3 platos semilla permiten probar un cobro en vivo inmediatamente después del registro.

### Negativas / Compensaciones
- Requiere actualizar el esquema de base de datos para admitir `tenants.nit_or_tax_id` como `NULLABLE` y agregar la columna `app_users.full_name`.

## Documentos Relacionados
- [ADR-0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica](./0002_hybrid_tenancy_and_dynamic_settings.md)
- [ADR-0006: Manejo Estricto de Fechas y Zonas Horarias](./0006_datetime_and_timezone_handling.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](./0008_unified_multilevel_parameter_system.md)
- [ADR-0027: Identidad de Plataforma y Bootstrap Limpio Go-Live](./0027_platform_identity_and_clean_golive_bootstrap.md)
- [Casos de Uso de Onboarding](../project/architecture/03-use-cases/use_cases_onboarding.md)
- [Casos de Uso de Empresa y Sucursales](../project/architecture/03-use-cases/use_cases_company.md)
- [Secuencia de Onboarding de Tenant](../project/architecture/04-behavior-and-flows/sequence_tenant_onboarding.md)
- [Estado del Ciclo de Vida del Tenant](../project/architecture/04-behavior-and-flows/state_tenant_lifecycle.md)
