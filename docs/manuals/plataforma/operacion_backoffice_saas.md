---
title: "Manual de Operación del Backoffice SaaS (Soporte y Administración Comercial)"
category: "platform-manual"
module: "backoffice"
tags:
  - manual
  - backoffice
  - saas
  - inquilinos
  - suscripciones
  - soporte
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 🛠️ Manual de Operación del Backoffice SaaS

> ⬅️ **Índice:** [Bóveda Central (`docs/README.md`)](../../README.md) | [Sub-Bóveda Backoffice](../../backoffice/README.md) | [Bootstrap y Puesta en Marcha](./bootstrap_y_operacion_inicial.md) | [Manuales Restaurante](../restaurante/README.md)

Guía operativa para el equipo de **administración SaaS, cobranzas y soporte técnico** sobre cómo utilizar la consola **Control Plane Backoffice** (`admin.localhost` / `ADMIN_DOMAIN`), totalmente separada del ERP de los restaurantes.

---

## 1. Roles y Matriz de Permisos en Backoffice

| Capacidad / Pantalla | `PLATFORM_OPERATOR` (`OPERADOR`) | `SUPERADMIN` |
| :--- | :---: | :---: |
| **`Dashboard`** (KPIs, recaudación, mora, alertas over-quota) | ✅ | ✅ |
| **`Inquilinos`** (buscar, alta asistida con `Nuevo Inquilino`, ficha 360°) | ✅ | ✅ |
| **Asignar planes, otorgar gracia y registrar pagos/abonos FIFO** | ✅ | ✅ |
| **Ver usuarios del tenant y telemetría de ventas** | ✅ | ✅ |
| **Probar conectividad de Storage activo** (`Probar Conexión`) | ✅ | ✅ |
| **`Observabilidad & Logs`**: `Salud`, `Métricas`, `Loggers en Caliente` | ✅ | ✅ |
| **Restablecer clave / activar / desactivar usuarios de un restaurante** | ❌ | ✅ |
| **Sobreescribir cuotas, suspender, reactivar, condonar cargos o cambiar política al vencer** | ❌ | ✅ |
| **Crear / editar / activar planes en `Planes SaaS`** | ❌ | ✅ |
| **Modificar parámetros en `Storage & Config`** (`platform_settings`) | ❌ | ✅ |
| **Ver `Configuración Activa` y `Auditoría Inmutable` en Observabilidad** | ❌ | ✅ |
| **Gestionar equipo en `Admins Plataforma`** | ❌ | ✅ |

---

## 2. Acceso y Primer Ingreso (`/login` y `/profile`)

1. Ingresa en la URL del Backoffice (`Ingresar al Backoffice`) usando tu **Usuario o Correo Electrónico** y contraseña.
2. Si tu cuenta fue recién creada o recuperada con contraseña temporal, el sistema te redirige obligatoriamente a **`Mi Perfil de Plataforma`** (`/profile`) mostrando el aviso *"Cambio de contraseña obligatorio"*.
3. Ingresa la clave temporal y tu nueva contraseña respetando la longitud mínima mostrada en pantalla (por defecto `12` caracteres; configurable en `security.platform_password_min_length`). Una vez actualizada, quedarás habilitado para operar el **`Dashboard`**.

---

## 3. Resumen Ejecutivo SaaS (`Dashboard`)

Al ingresar verás los cuatro indicadores consolidados de la plataforma:
- **`Inquilinos Totales`**: conteo total de restaurantes con desglose de activos y en prueba (*trial*).
- **`Recaudado Este Mes`**: pagos de suscripción registrados en el mes vigente.
- **`Mora / Saldo Pendiente`**: suma de cargos pendientes o con pago parcial.
- **`Alertas Over-Quota`**: cantidad de inquilinos que exceden límites tras un cambio de plan (*downgrade*).

---

## 4. Cartera de Inquilinos y Alta Asistida (`Inquilinos`)

### A. Filtrado y Semáforo Comercial
En **`Inquilinos`** puedes buscar por nombre legal, NIT o slug, filtrar por estado (`Activos`, `En Trial`, `En Gracia`, `Suspendidos`) y activar los filtros rápidos **`Solo con deuda`** o **`Excedidos en cuota`**.

Observa especialmente las insignias en la columna **`Estado / Bloqueo`**:
- **`Sin primer ingreso`** *(V20)*: el restaurante fue pre-aprovisionado desde Backoffice o aún nadie de su equipo ha iniciado sesión por primera vez en el ERP.
- **`Bloqueo POS / Turno`** o **`Bloqueo Total`**: restricción operativa vigente.
- **`Excedido: ...`**: alerta indicando qué recurso (sucursales, usuarios, platos, insumos u órdenes mensuales) supera la cuota del plan vigente.

### B. Alta Asistida (`Nuevo Inquilino`)
Cuando necesites entregar un restaurante listo al cliente:
1. Pulsa **`Nuevo Inquilino`**.
2. Completa **Empresa** (nombre legal, slug auto-sugerido editable y NIT opcional), **Sucursal Principal** (nombre, dirección y huso horario IANA, por defecto `America/La_Paz`), **Plantilla Operativa** (`TRADITIONAL`, `FAST_FOOD`, `CAFE` o `EMPTY` + casilla de catálogo demo) y **Usuario Administrador** (nombre completo, usuario, correo electrónico —opcional si las reglas lo permiten— y contraseña inicial).
3. El restaurante quedará creado con suscripción inicial (`TRIAL_14D` por defecto) y marcado como **`Sin primer ingreso`** hasta que su dueño inicie sesión en el ERP.

---

## 5. Ficha 360° del Inquilino (`/tenants/:id`)

Al hacer clic sobre cualquier restaurante en la cartera se abre su ficha integral con 5 pestañas:

### Pestaña 1: `Resumen, Cuotas & Sucursales`
- Muestra barras de consumo actual frente a los topes en las 5 dimensiones: **Sucursales**, **Usuarios**, **Platos**, **Insumos** y **Órdenes/Mes**.
- **Sobreescritura no destructiva (solo `SUPERADMIN`):** Si un cliente excede su cuota o negoció un tope a medida, puedes ingresar límites personalizados por recurso (o dejar vacío para ilimitado). **Regla de oro:** bajar una cuota o cambiar de plan nunca borra platos, usuarios ni sucursales del restaurante; únicamente bloquea crear nuevos registros por encima del tope.

### Pestaña 2: `Usuarios` *(Soporte de Accesos al ERP)*
- Permite buscar y listar todos los usuarios internos de ese restaurante (dueño, encargados, cajeros).
- **`Restablecer Clave` (solo `SUPERADMIN`):** Cuando un administrador de restaurante olvida su contraseña, esta acción genera una clave temporal sugerida (con botón de copiado rápido), **revoca todas las sesiones activas** de ese usuario en el ERP y marca obligatoriamente el cambio de contraseña para su próximo ingreso.
- **`Activar` / `Desactivar` (solo `SUPERADMIN`):** Habilita o suspende un usuario específico sin tocar los datos del resto del equipo.

### Pestaña 3: `Telemetría & Ventas`
- Consulta las métricas reales de operación del restaurante filtrando por rango de calendario local (`Desde (Calendario Local)` y `Hasta`) y pulsando **`Consultar Rango`**:
  - **Ventas Brutas**, **Órdenes Pagadas**, **Ticket Promedio**, **Órdenes Anuladas** y gráfico diario.
- Sirve para soporte comercial, validar adopción tras el alta y revisar actividad antes de aplicar cortes o bloqueos.

### Pestaña 4: `Planes, Gracia & Bloqueos`
1. **Asignar o Cambiar Plan:** Selecciona un plan activo y elige la modalidad:
   - **`Pago recibido al contado`**: registra el cargo como pagado (`Pagado`), emite recibo e ingresa dinero de inmediato (`QR`, `TRANSFER`, `CASH`, `CARD`, `OTHER` + código de referencia).
   - **`Uso por adelantado / Crédito`**: activa el plan dejando un cargo **`Pendiente / Mora`** en la cuenta corriente con fecha límite opcional.
2. **Período de Gracia (`Activar Período de Gracia`):** Permite extender el servicio en casos de compromiso de pago fijando una fecha/hora límite (`graceUntil`), un tope máximo de ventas permitidas durante la gracia (`graceMaxOrders`), o ambos.
3. **Suspensión Manual y Reactivación (solo `SUPERADMIN`):**
   - **`POS_AND_SHIFT_ONLY` (`Bloqueo POS / Turno`):** el personal aún puede ingresar al ERP para consultar reportes o regularizar datos, pero no puede abrir cajas ni cobrar en el POS.
   - **`TOTAL` (`Bloqueo Total`):** bloquea por completo el inicio de sesión y revoca todas las sesiones ERP activas del restaurante.
   - **Reactivación y Política al Vencer:** permite reactivar manualmente (opcionalmente sumando días de vigencia) y definir la política automática que se aplicará si vence el período sin pago (`POS_AND_SHIFT_ONLY` o `TOTAL`).

### Pestaña 5: `Cuenta Corriente & Pagos`
- Muestra los cargos históricos (`Pagado`, `Pago Parcial`, `Pendiente / Mora`, `Condonado`) y los pagos recibidos.
- **`Registrar Pago / Abono FIFO`:** Registra abonos totales o parciales. Si no seleccionas un cargo específico, el sistema aplica **conciliación FIFO automática** cubriendo primero los cargos vencidos más antiguos; si la casilla está activa, al quedar saldo `0` puede reactivar automáticamente la suscripción.
- **`Condonar` (solo `SUPERADMIN`):** Anula administrativamente un cargo abierto (`Condonado`) sin ingresar efectivo.

---

## 6. Catálogo de Planes SaaS (`Planes SaaS`)

- Usa los filtros por categoría (`Todos`, `Públicos (Onboarding)`, `Prueba (Trial)`, `Especiales / Privados`) para distinguir:
  - **`PUBLIC_ONBOARDING`:** planes visibles en el flujo comercial público.
  - **`TRIAL`:** planes de prueba inicial.
  - **`CUSTOM_PRIVATE`:** planes privados negociados a medida, asignables solo desde el Backoffice.
- Como **`SUPERADMIN`**, al pulsar **`Nuevo Plan`** o **`Editar`** puedes:
  - Definir **`Días (0 = Ilimitado)`** (`0` configura un plan **Sin vencimiento**, ideal para cuentas internas, cortesías o contratos anuales especiales).
  - Dejar vacíos los topes numéricos para otorgar cupo **Ilimitado**.
  - Usar el atajo **`Preset: Plan Gratis Ilimitado`** para rellenar en un clic un plan privado gratuito, sin vencimiento y sin límites.
  - Activar o desactivar planes sin afectar a los clientes que ya los tienen asignados.

---

## 7. Storage, Políticas y Observabilidad (`Storage & Config` / `Observabilidad & Logs`)

### A. `Storage & Config` (`/platform/settings`)
- **Prueba en vivo:** tanto `PLATFORM_OPERATOR` como `SUPERADMIN` pueden pulsar **`Probar Conexión (Proveedor Activo)`** para verificar si la escritura multimedia responde correctamente.
- **Parámetros globales (solo `SUPERADMIN`, con invalidación inmediata de caché al guardar):**
  - `storage.active_provider`: `LOCAL` (disco local) o `S3` (OCI Object Storage / S3-Compatible).
  - `storage.max_file_size_bytes`: tamaño máximo por imagen de plato (por defecto `1048576` bytes = 1 MB).
  - `storage.public_url_prefix`: prefijo público para construir las URLs de imágenes.
  - `security.platform_password_min_length`: longitud mínima de contraseñas para administradores de plataforma (por defecto `12`, rango UI 6–64).
  - `security.platform_password_require_complexity`: exige mayúsculas/minúsculas/números/símbolos adicionales (`false` por defecto).

### B. `Observabilidad & Logs` (`/platform/observability`)
Dispone de 5 pestañas según rol:
1. **`Salud del Sistema`**: estado global (`UP`/`DOWN`) de base de datos, disco y subsistemas.
2. **`Métricas JVM & HTTP`**: memoria JVM, hilos, latencias HTTP y pool de conexiones HikariCP.
3. **`Loggers en Caliente`**: permite subir temporalmente un paquete (por defecto `com.restaurant.app`) a `DEBUG` o `TRACE` para diagnosticar incidencias sin reiniciar; el sistema muestra alerta visual si dejaste loggers verbosos activos para recordar devolverlos a `INFO`.
4. **`Configuración Activa`** *(solo `SUPERADMIN`)*: inspección enmascarada de propiedades activas.
5. **`Auditoría Inmutable`** *(solo `SUPERADMIN`)*: bitácora paginada filtrable por actor, acción y texto de detalle, con inspector JSON de cada evento administrativo.

---

## 8. Administración del Equipo Backoffice (`Admins Plataforma`)

Exclusivo para **`SUPERADMIN`**:
- Permite listar el equipo administrativo de la plataforma, ver su último acceso y activar/desactivar cuentas (`Activar` / `Desactivar`).
- Al crear un nuevo miembro (`PLATFORM_OPERATOR` o `SUPERADMIN`), su correo electrónico es **opcional** (*V22*), se valida la contraseña inicial contra la política vigente y el nuevo administrador queda marcado para cambiar su clave obligatoriamente al ingresar por primera vez.

---

## Relacionado

- [Manual de Bootstrap y Operación Inicial](./bootstrap_y_operacion_inicial.md)
- [Flujo de Alta de Empresa y Onboarding (Procesos de Negocio)](../../business-processes/04-administracion-y-personal/02_flujo_alta_empresa_y_onboarding.md)
- [Manuales de Usuario Final para Restaurantes](../restaurante/README.md)
- [Sub-Bóveda Técnica del Backoffice](../../backoffice/README.md)
- [Especificación Frontend React del Backoffice](../../backoffice/sdd/04_backoffice_frontend_react_spec.md)
