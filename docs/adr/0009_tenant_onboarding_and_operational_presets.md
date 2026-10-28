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
last_updated: "2026-10-28"
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
  El slug se deriva en tiempo real en el frontend a partir del nombre comercial (`slugify`). La interfaz muestra un botón/tooltip informativo explicativo ("¿Qué es este identificador?") y permite al usuario personalizarlo, validando su disponibilidad asíncronamente (`GET /api/v1/auth/check-slug?slug=...`).

### 2. Plantillas de Negocio y Platos Semilla (Catálogo Demo)
El asistente ofrece cuatro opciones claramente diferenciadas para arrancar:
1. **Comida Rápida / Pizzería (*Fast Food*):** Venta rápida en mostrador, modo para llevar por defecto, descuento de inventario permisivo y 3 platos demo con recetas simples (ej. Hamburguesa Clásica, Pizza Personal, Refresco).
2. **Restaurante Tradicional / A la Carta:** Consumo en salón por defecto, fotos de platos visibles, notas/exclusiones de cocina ("Sin cebolla") y 3 platos demo (Entrada, Plato Fuerte, Postre).
3. **Cafetería / Bar:** Servicio en barra, Happy Hour preconfigurado, agilidad sin PIN de supervisor y 3 productos demo (Café Especial, Sandwich/Snack, Bebida).
4. **Plantilla Vacía (Personalizada):** Para usuarios avanzados que prefieren arrancar con el catálogo en blanco y cargar sus datos manualmente o mediante archivo Excel/CSV.

### 3. Localización y Sucursal Matriz
- **Huso Horario Inadvertido:** Se infiere automáticamente a nivel de sistema (`branches.timezone = 'America/La_Paz'` según [ADR-0006](./0006_datetime_and_timezone_handling.md)) sin abrumar al comensal con selectores técnicos.
- **Moneda Base:** Se presenta por defecto en Bolivianos (`BOB`), permitiendo cambiar a otra divisa si lo desea.
- **Georreferenciación Libre:** Dirección referencial con mapa interactivo opcional basado en tecnologías abiertas (OpenStreetMap / Leaflet), sin costos de licencias ni dependencias de APIs privativas. Teléfono de contacto opcional.
- **Logotipo Opcional:** Admite carga de imagen con fallback a iniciales y paleta corporativa si se omite.

### 4. Administrador Propietario
Se solicitan:
- Nombre completo del propietario (`app_users.full_name`).
- Nombre de usuario único en la empresa (`username`).
- Correo electrónico de contacto (opcional pero incentivado con mensaje de recuperación de acceso).
- Contraseña con cifrado Argon2id.

### 5. Resumen Previo y Entrada "Ready to Sell"
- Antes de registrar, un modal interactivo presenta el resumen de los datos introducidos aclarando: *"Esto es solo para iniciar; podrás ajustar o eliminar cualquier dato después"*.
- Tras la confirmación, el backend ejecuta una transacción atómica `@Transactional` (creación de Tenant + Branch + User Admin + Presets semilla con Sparse Storage + Platos demo) y emite un token JWT que redirige directamente al panel operativo.

---

## Consecuencias

### Positivas
- **Cero Fricción para PyMEs:** Negocios informales o pequeños se registran en menos de 2 minutos sin exigirles NIT ni correo corporativo obligatorio.
- **Sinergia con Sparse Storage (ADR-0008):** La nueva empresa no satura la base de datos con tablas de configuración; adopta presets en $O(1)$.
- **Prueba Inmediata del POS:** Los 3 platos semilla permiten probar un cobro en vivo inmediatamente después del registro.

### Negativas / Compensaciones
- Requiere actualizar el esquema de base de datos para admitir `tenants.nit_or_tax_id` como `NULLABLE` y agregar la columna `app_users.full_name`.
