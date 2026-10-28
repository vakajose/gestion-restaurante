---
title: "ADR 0010: Control de Acceso RBAC Dinámico, Asignación Multi-Sucursal y Reseteo Delegado"
category: "adr"
tags:
  - adr
  - architecture
  - security
  - rbac
  - multi-tenant
  - multi-branch
  - auth
last_updated: "2026-10-28"
status: "accepted"
---

# ADR 0010: Control de Acceso RBAC Dinámico, Asignación Multi-Sucursal y Reseteo Delegado

## Contexto
El sistema gestiona accesos mediante roles básicos (`ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`). Sin embargo, en el sector gastronómico real:
1. **Diversidad de Perfiles Operativos:** Una pizzería requiere un rol de *"Jefe de Horno"* con acceso exclusivo a KDS de cocina; un restaurante a la carta necesita un *"Capitán de Mozos"* que autorice cortesías sin acceso a caja chica; un bar requiere *"Bartender"* y una cadena requiere *"Auditor Nocturno"* o *"Encargado de Compras"*.
2. **Riesgo de Sobreingeniería:** Modelos complejos tipo Oracle o AWS IAM con *Direct Grants por usuario* y excepciones individuales superpuestas generan una curva de aprendizaje excesiva y confusión administrativa para dueños de restaurante.
3. **Personal Rotativo Multi-Sucursal:** En cadenas y franquicias, colaboradores como cajeros y meseros rotan entre locales según el día de la semana, y los supervisores de zona gestionan un grupo de sucursales ($2$ o $3$) sin tener acceso a toda la cadena. La asignación actual de un único `branch_id` por usuario es insuficiente.
4. **Colaboradores Temporales:** Se contratan mozos o ayudantes por temporada alta (fin de año, festividades) o se delegan encargaturas interinas durante vacaciones, requiriendo vigencia temporal automática en los roles.
5. **Cero Dependencia de Correo Electrónico:** La mayoría de operadores de mostrador y cocina no poseen correo corporativo ni lo revisan en el restaurante, haciendo inviable el autorreseteo de contraseñas por correo.

---

## Decisión

Se adopta una arquitectura de **Control de Acceso Basado en Roles (RBAC) Pragmático, Asignación Multi-Sucursal y Reseteo Delegado** (`PB-039` / `TASK-336`):

### 1. Modelo RBAC Pragmático de 2 Niveles (Sin Sobreingeniería)
Para mantener simplicidad y máximo rendimiento:
- **Nivel 1 (Permisos Atómicos en Código):** Las acciones y recursos protegidos se definen de forma tipada en `SystemPermissions.java` (`pos:access`, `pos:order_create`, `pos:discount_apply`, `finance:expenses_approve`, `inventory:adjust`, etc.).
- **Nivel 2 (Roles Dinámicos por Empresa):** Entidad `roles` vinculada al tenant (`tenant_id`), con permisos asociados en `role_permissions (role_id, permission)`.
- **Agrupación Visual de Negocio en Frontend:** En lugar de crear entidades pesadas en base de datos para "Privilegios", la interfaz de TailwindAdmin agrupa los permisos en módulos operativos claros (Punto de Venta, Cocina, Caja, Caja Menor, Inventario, Catálogo, Personal, Configuración).

### 2. Inmutabilidad y Protección Anti-Bloqueo de `ADMIN_TENANT`
- El rol `ADMIN_TENANT` es nativo del sistema (`is_system = true`).
- Posee permanentemente todos los permisos de la empresa.
- Queda terminantemente prohibido a nivel de API REST y UI despojarle de permisos, restringirle sucursales o eliminarlo.

### 3. Asignación Multi-Sucursal y Personal Rotativo
- Se introduce la tabla relacional `user_branches (user_id, branch_id)`.
- La columna `app_users.branch_id` se mantiene como `default_branch_id` (sucursal predeterminada al ingresar).
- Al autenticarse, el backend resuelve todas las sucursales donde el colaborador está autorizado y las incluye en el claim `branch_ids` del token JWT.
- El usuario conmuta libremente entre sus sucursales asignadas usando el selector del Topbar (`TASK-201`), transmitiendo la cabecera `X-Branch-Id`, validada en $O(1)$ por `JwtAuthenticationFilter`.

### 4. Vigencia Temporal de Roles (`role_valid_until`)
- Se añade la columna `app_users.role_valid_until TIMESTAMPTZ NULLABLE`.
- Si es nulo, el rol es permanente.
- Si `role_valid_until < NOW()`, el rol ha vencido. El backend rechaza peticiones con `HTTP 403 Forbidden` (`ProblemDetail`), y el panel administrativo destaca visualmente la alerta de expiración.

### 5. Circuito Jerárquico de Reseteo Asistido de Contraseñas
Para prescindir de correos electrónicos en operaciones de salón/cocina:
- **Superadmin:** Puede resetear credenciales del administrador `ADMIN_TENANT`.
- **Administrador de Restaurante:** Puede resetear contraseñas de cualquier colaborador de su empresa (`POST /api/v1/users/{id}/reset-password`).
- **Encargado de Sucursal (`BRANCH_MANAGER`):** Puede resetear contraseñas de cajeros, meseros y cocineros asignados a sus sucursales autorizadas.
- **Forzado de Cambio:** El reseteo activa `must_change_password = true` en `app_users`. Al ingresar con la clave temporal, la interfaz despliega forzosamente un modal que obliga al usuario a establecer su contraseña privada antes de entrar al POS o panel.

### 6. Mitigación de Token Bloat en JWT
- Usuarios administradores reciben el claim comodín `permissions: ["*"]`.
- Usuarios operativos reciben la lista exacta de permisos activos concedidos por su rol.
- Directivas y Route Guards en Angular Zoneless evalúan `permissions.includes('*') || permissions.includes(requiredPermission)` en tiempo constante $O(1)$.

---

## Consecuencias

### Positivas
- **Flexibilidad total para el restaurante:** Permite crear roles específicos (ej. "Jefe de Barra", "Mozo Fin de Semana") sin alterar el código ni la base de datos central.
- **Soporte nativo para rotación de personal:** Cajeros y supervisores pueden operar en múltiples sucursales sin duplicar cuentas de usuario.
- **Cero bloqueos por olvido de contraseña:** Los encargados resuelven pérdidas de clave en el mismo local en menos de 10 segundos sin requerir correos electrónicos.
- **Protección contra errores:** Imposibilidad de auto-bloqueo del administrador principal.

### Negativas / Mitigaciones
- **Invalidación ante cambio de rol:** Si a un cajero se le retiran permisos de cobro en mitad de su jornada, su token JWT actual mantendrá los permisos hasta su expiración o renovación.  
  *Mitigación:* El endpoint de actualización de rol revoca las sesiones activas del usuario o se fuerza el refresco mediante `JwtService` ([ADR-0003](0003_offline_first_dexie_sync.md) / [ADR-0008](0008_unified_multilevel_parameter_system.md)).
