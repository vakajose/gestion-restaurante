# ADR-0024: Perfil de Usuario Autoservicio, Gestión de Contacto y PIN, y Humanización de Personal (PB-045)

- **Estado:** `ACEPTADO`
- **Fecha:** 2026-11-09
- **Autores:** Squad Multi-Agente & Líder de Proyecto (Co-Diseño Interactivo)
- **Alcance:** Autenticación & Seguridad (`core.security`), Perfil de Usuario Autoservicio, Gestión de Personal (`/users`), UX/UI Humanizada
- **Requerimiento:** `PB-045` (Perfil de Usuario Autoservicio, Visualización de Nombres Reales y Limpieza de Privilegios Técnicos en UI)
- **Tareas Relacionadas:** `TASK-509` [AUTH] [UI/UX]

---

## 1. Contexto y Problemática

En versiones previas del sistema, la gestión de usuarios y perfiles presentaba limitaciones de usabilidad, seguridad y presentación visual:
1. **Falta de Autoservicio de Datos de Contacto:** El endpoint de perfil (`PUT /api/v1/profile`) únicamente permitía modificar la dirección de correo electrónico. Los usuarios no podían corregir su nombre real (`fullName`) ni registrar un teléfono de contacto (`phone_number`).
2. **Ruido Visual y Privilegios Crudos:** En la pantalla "Mi Perfil" (`/profile`) se desplegaba una lista cruda de strings técnicos de permisos (ej. `menu:read`, `pos:orders:create`, `finance:shifts:close`), generando confusión para el usuario común y degradando la estética profesional del panel.
3. **Omisión de Nombres Reales:** En el sidebar, en el header y en la tabla de personal (`/users`), se priorizaba el nombre de usuario técnico (ej. `carlos_admin`) en negrita, dejando el nombre real relegado o ausente.
4. **Administración Incompleta de Colaboradores:** Al registrar o editar personal en `/users`, los administradores no contaban con campos para gestionar el nombre completo ni el teléfono del colaborador.
5. **Gestión Descentralizada del PIN:** Con la introducción de autorizaciones rápidas in-situ mediante PIN (ADR-0019), los supervisores y administradores requieren un espacio autoservicio dentro de su perfil para configurar y actualizar su clave numérica de 4 a 6 dígitos.

---

## 2. Decisiones Arquitectónicas

### A. Extensión del Modelo Relacional (`app_users` - Migración Flyway V16)
- Se incorpora la columna `phone_number VARCHAR(30)` en la tabla `app_users` (migración `V16__user_phone_number.sql`).
- La entidad `AppUser.java` y los DTOs `UserDto`, `CreateUserRequest`, `UpdateUserRequest` y `UpdateProfileRequest` integran los campos `fullName` y `phoneNumber`.

### B. Endpoints de Autoservicio de Perfil (`ProfileController`)
- **`GET /api/v1/profile`:** Retorna el `UserDto` completo del usuario autenticado, enriquecido con `fullName`, `phoneNumber`, `roleDisplayName` y nombres legibles de sucursales autorizadas.
- **`PUT /api/v1/profile`:**
  - Recibe `UpdateProfileRequest`:
    ```java
    public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 30) String phoneNumber,
        @Email String email
    ) {}
    ```
  - Actualiza directamente los datos personales sin requerir contraseña actual (fricción cero para datos de contacto).
- **`PUT /api/v1/profile/password`:**
  - Requiere `currentPassword`, `newPassword` y `confirmPassword`. Valida la credencial actual contra Argon2id antes de actualizar.
- **`PUT /api/v1/profile/pin`:**
  - Recibe `UpdatePinRequest` con un PIN numérico de 4 a 6 dígitos.
  - Almacena el hash BCrypt en `app_users.pin_hash`.

### C. Rediseño Limpio y Ejecutivo de "Mi Perfil" (`/profile`)
1. **Eliminación de la Lista Cruda de Permisos:**
   - Se suprime la visualización de tokens técnicos de recursos (`module:action`).
   - Se reemplaza por una **Tarjeta Ejecutiva de Rol y Cobertura**:
     - Badge jerárquico legible ("Administrador de Empresa", "Gerente de Sucursal", "Cajero", "Encargado de Cocina").
     - Lista de sucursales autorizadas con nombres de local legibles (ej. "Central Mall", "Sucursal Norte").
     - Indicador de vigencia temporal de acceso (si aplica).
2. **Formularios Autoservicio:**
   - *Tarjeta 1 (Datos de Contacto):* Inputs para `fullName`, `phoneNumber` y `email` con guardado inmediato.
   - *Tarjeta 2 (Credenciales de Acceso):* Cambio seguro de contraseña.
   - *Tarjeta 3 (PIN de Autorización Rápida):* Configuración de PIN de 4-6 dígitos para usuarios con permisos de supervisión.

### D. Humanización en Sidebar, Header y Tabla de Personal (`/users`)
1. **Sidebar y Header:**
   - El texto principal visible es `user.fullName` en negrita (con fallback a `user.username` solo si no tiene nombre registrado).
   - Debajo, se despliega el título legible del rol en badge discreto ("Administrador", "Cajero", etc.).
2. **Tabla de Personal (`/users`):**
   - Columna "Colaborador": Avatar con iniciales del nombre real, `fullName` en negrita como texto primario, y `@username` / email en texto secundario tenue.
   - Modales de creación y edición de colaboradores en `/users`: Incorporan campos `fullName` y `phoneNumber`.
   - Barra de búsqueda de personal: Filtra reactivamente por `fullName`, `username` o `email`.

---

## 3. Consecuencias y Beneficios

- **Profesionalismo y Ergonomía:** La interfaz abandona códigos técnicos crudos y refleja la identidad real de las personas que operan el restaurante.
- **Autonomía Operativa:** Los empleados pueden mantener actualizados sus teléfonos de contacto y gestionar su contraseña y PIN sin requerir intervención de soporte.
- **Gestión Integral de RRHH:** El administrador dispone de nombres y teléfonos unificados en la ficha de cada colaborador.
- **Seguridad Preservada:** El cambio de contraseña continúa blindado con verificación obligatoria de la clave previa.
