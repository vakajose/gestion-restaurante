# ADR 0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica

## Contexto
El sistema atiende tanto a restaurantes individuales (una sola sucursal) como a cadenas gastronómicas con múltiples sucursales. Además, el mercado objetivo abarca desde micro/pequeñas empresas que operan de manera informal (usuarios sin correo electrónico corporativo, inventario manual no automatizado por comanda) hasta medianas empresas con controles estrictos. 

Si el esquema de base de datos impone columnas rígidas para cada opción de configuración o si fuerza correos electrónicos obligatorios y reglas inflexibles de inventario, el producto se vuelve incompatible con la realidad operativa de sus usuarios o exigirá migraciones constantes de base de datos por cada nuevo parámetro.

## Decisión
1. **Jerarquía Dual de Entidades:**
   - `BaseTenantEntity` (`tenantId`, `createdAt`, `updatedAt`, `version`) para catálogo maestro y entidades globales.
   - `BaseBranchEntity extends BaseTenantEntity` (`branchId`) para transacciones y operaciones de sucursal.
2. **Denormalización de Índices de Alto Rendimiento:**
   - Se incluyen `tenant_id` y `branch_id` en tablas transaccionales hijas (`order_items`, `purchase_items`) para optimizar índices compuestos e independizar consultas sin joins innecesarios.
3. **Identidad Adaptativa:**
   - `username` es la credencial obligatoria y única por tenant (`uq_user_tenant_username`).
   - `email` es opcional (nullable), permitiendo dar de alta cajeros u operadores sin correo.
4. **Configuración Dinámica Clave-Valor:**
   - Se crean `tenant_settings` y `branch_settings` con pares `(setting_key, setting_value, value_type)`.
   - Se abstrae el acceso mediante `TenantConfigService` con valores por defecto en código.

## Consecuencias
- **Positivas:** Máxima flexibilidad para incorporar nuevos parámetros sin alterar el esquema relacional; adaptación total al mercado PyME; alto rendimiento en consultas multi-sucursal.
- **Negativas:** La configuración clave-valor requiere tipado y validación rigurosa en la capa de servicio backend.
