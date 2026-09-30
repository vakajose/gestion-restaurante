# 04. Esquema de Base de Datos y Modelo Físico (PostgreSQL)

## 1. Convenciones y Extensiones
- **Motor objetivo:** PostgreSQL 16+
- **Tipos de clave primaria:** `UUID` con función por defecto `gen_random_uuid()`.
- **Campos monetarios:** `NUMERIC(12, 2)`
- **Cantidades e insumos:** `NUMERIC(12, 4)`
- **Control de concurrencia optimista:** `version BIGINT DEFAULT 0`.
- **Estrategia Multi-Tenant de Alto Rendimiento:**
  - Tablas maestras de empresa incluyen `tenant_id` y aislamiento a nivel tenant.
  - Tablas operativas y de detalle (`orders`, `order_items`, `purchases`, `purchase_items`, `expenses`, `kardex_movements`) denormalizan `tenant_id` y `branch_id` para habilitar índices compuestos de alto rendimiento (`CREATE INDEX idx_<table>_tenant_branch ON <table>(tenant_id, branch_id);`), facilitando particionamiento físico y consultas analíticas sin necesidad de `JOIN`s profundos.

---

## 2. Entidades Principales del Modelo

1. **`tenants`:** Empresas registradas (razón social, NIT/tax ID, estado).
2. **`tenant_settings`:** Configuración dinámica clave-valor por empresa (modo de inventario, políticas de email, permisos de sucursal).
3. **`branches`:** Sucursales físicas de la empresa.
4. **`branch_settings`:** Configuración dinámica clave-valor por sucursal (impresión automática, pedidos offline).
5. **`app_users`:** Cuentas de usuario con `username` único por tenant, `email` opcional (nullable) para cajeros/operadores y roles tipados.
6. **`user_preferences`:** Preferencias individuales de UI (tema Dark/Light, idioma).
7. **`categories`:** Agrupaciones del catálogo a nivel empresa.
8. **`ingredients`:** Insumos base con unidad de medida y stock mínimo de alerta (el costo promedio ponderado vive por sucursal en `branch_stock`).
9. **`dishes`:** Platos y productos de venta. Soporta platos maestros a nivel tenant, platos clonados con trazabilidad (`cloned_from_id`) y platos exclusivos creados en sucursal (`branch_id`).
10. **`dish_recipes`:** Relación N:M entre platos e insumos con gramajes por ración (BOM - Bill of Materials).
11. **`branch_dishes`:** Reglas de sobreescritura de precio y disponibilidad (`is_available`, `price_override`) para platos maestros en sucursales.
12. **`branch_stock`:** Existencias físicas y costo unitario promedio ponderado por ingrediente en cada sucursal.
13. **`purchases` / `purchase_items`:** Registro de facturas de compras de insumos a proveedores.
14. **`kardex_movements`:** Bitácora inmutable de movimientos de almacén (compras, salidas por venta, mermas, inventarios iniciales, conteos físicos).
15. **`orders` / `order_items`:** Tickets de comanda y sus líneas de detalle, con soporte de idempotencia vía `client_transaction_id` y vinculación al turno de caja.
16. **`cash_shifts`:** Turnos de caja (apertura, totales por método de pago, egresos aprobados, efectivo esperado vs. real y descuadre).
17. **`expenses`:** Gastos operativos diarios y costos fijos prorrateados, con flujo de aprobación (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`) y evidencia digital para afectar la caja chica.
18. **`audit_logs`:** Trazabilidad inmutable de acciones críticas con `old_values` y `new_values` en formato JSONB.

---

## 3. Esquema DDL Completo (PostgreSQL)

### A. Módulo de Seguridad, Tenancy y Configuración Dinámica

```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name VARCHAR(150) NOT NULL,
    nit_or_tax_id VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Configuración dinámica clave-valor por Tenant (evita migraciones continuas por nuevos parámetros)
CREATE TABLE tenant_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' CHECK (value_type IN ('STRING', 'BOOLEAN', 'NUMBER', 'JSON')),
    description VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_setting_key UNIQUE (tenant_id, setting_key)
);
CREATE INDEX idx_tenant_settings_lookup ON tenant_settings(tenant_id, setting_key);

-- Claves estándar reconocidas en tenant_settings:
-- 'INVENTORY_DEDUCTION_MODE': 'AUTOMATIC_PERMISSIVE' | 'AUTOMATIC_STRICT' | 'MANUAL_ISOLATED'
-- 'REQUIRE_USER_EMAIL': 'true' | 'false' (default 'false')
-- 'REQUIRE_EMAIL_VERIFICATION': 'true' | 'false' (default 'false')
-- 'ALLOW_BRANCH_DISH_OVERRIDE': 'true' | 'false' (default 'true')
-- 'ALLOW_BRANCH_EXCLUSIVE_DISHES': 'true' | 'false' (default 'true')
-- 'REQUIRE_SHIFT_EXPENSE_APPROVAL': 'true' | 'false' (default 'true')

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    phone VARCHAR(30),
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/La_Paz', -- Huso horario local de operación
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_branches_tenant ON branches(tenant_id);

-- Configuración dinámica clave-valor por Sucursal
CREATE TABLE branch_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' CHECK (value_type IN ('STRING', 'BOOLEAN', 'NUMBER', 'JSON')),
    description VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_branch_setting_key UNIQUE (branch_id, setting_key)
);
CREATE INDEX idx_branch_settings_lookup ON branch_settings(branch_id, setting_key);

-- Claves estándar reconocidas en branch_settings:
-- 'ALLOW_OFFLINE_ORDERS': 'true' | 'false' (default 'true')
-- 'AUTO_PRINT_KITCHEN_TICKET': 'true' | 'false' (default 'false')

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120), -- NULLABLE para permitir cajeros/cocineros sin correo
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('SUPERADMIN', 'ADMIN_TENANT', 'BRANCH_MANAGER', 'CASHIER', 'KITCHEN')),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_tenant_username UNIQUE (tenant_id, username)
);
CREATE INDEX idx_users_tenant_branch ON app_users(tenant_id, branch_id);
CREATE INDEX idx_users_email ON app_users(email) WHERE email IS NOT NULL;

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    theme VARCHAR(10) NOT NULL DEFAULT 'LIGHT' CHECK (theme IN ('LIGHT', 'DARK')),
    language VARCHAR(10) NOT NULL DEFAULT 'es'
);
```

---

### B. Módulo Catálogo y Recetas (BOM)

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    icon VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_categories_tenant ON categories(tenant_id);

CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    unit_of_measure VARCHAR(20) NOT NULL CHECK (unit_of_measure IN ('KG', 'GRAM', 'LITER', 'ML', 'UNIT')),
    min_stock_alert NUMERIC(12, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id);

CREATE TABLE dishes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE CASCADE, -- NULL: Plato Maestro del Tenant; NOT NULL: Exclusivo de Sucursal
    cloned_from_id UUID REFERENCES dishes(id) ON DELETE SET NULL, -- Trazabilidad al clonar un plato maestro
    category_id UUID NOT NULL REFERENCES categories(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    sale_price NUMERIC(12, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_dish_code_scope UNIQUE (tenant_id, branch_id, code)
);
CREATE INDEX idx_dishes_tenant_cat ON dishes(tenant_id, category_id);
CREATE INDEX idx_dishes_branch ON dishes(branch_id);

CREATE TABLE dish_recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity NUMERIC(12, 4) NOT NULL CHECK (quantity > 0),
    CONSTRAINT uq_dish_ingredient UNIQUE (dish_id, ingredient_id)
);
CREATE INDEX idx_recipes_dish ON dish_recipes(dish_id);

-- Reglas de disponibilidad y sobreescritura de precio por sucursal sobre platos maestros
CREATE TABLE branch_dishes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    price_override NUMERIC(12, 2), -- NULL hereda dishes.sale_price
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_branch_dish UNIQUE (branch_id, dish_id)
);
CREATE INDEX idx_branch_dishes_lookup ON branch_dishes(tenant_id, branch_id, is_available);
```

---

### C. Módulo Inventario, Compras y Kardex Físico

```sql
CREATE TABLE branch_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    current_quantity NUMERIC(12, 4) NOT NULL DEFAULT 0,
    average_unit_cost NUMERIC(12, 4) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_branch_ingredient UNIQUE (branch_id, ingredient_id)
);
CREATE INDEX idx_stock_tenant_branch ON branch_stock(tenant_id, branch_id);

CREATE TABLE purchases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    supplier_name VARCHAR(150),
    invoice_number VARCHAR(50),
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    purchase_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_purchases_tenant_branch ON purchases(tenant_id, branch_id);

CREATE TABLE purchase_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity NUMERIC(12, 4) NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC(12, 4) NOT NULL CHECK (unit_cost >= 0),
    subtotal NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_purchase_items_purchase ON purchase_items(purchase_id);
CREATE INDEX idx_purchase_items_tenant_branch ON purchase_items(tenant_id, branch_id);

CREATE TABLE kardex_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    movement_type VARCHAR(30) NOT NULL CHECK (
        movement_type IN (
            'PURCHASE_IN', 
            'SALE_OUT', 
            'WASTE_ADJUSTMENT', 
            'TRANSFER_IN', 
            'TRANSFER_OUT', 
            'INITIAL_INVENTORY', 
            'PHYSICAL_COUNT_ADJUSTMENT'
        )
    ),
    quantity NUMERIC(12, 4) NOT NULL,
    unit_cost NUMERIC(12, 4) NOT NULL,
    total_cost NUMERIC(12, 2) NOT NULL,
    balance_quantity NUMERIC(12, 4) NOT NULL,
    reference_id UUID, -- ID de purchase_id, order_id o inventario físico
    movement_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES app_users(id)
);
CREATE INDEX idx_kardex_lookup ON kardex_movements(tenant_id, branch_id, ingredient_id, movement_date DESC);
```

---

### D. Módulo Punto de Venta (POS)

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    cash_shift_id UUID, -- Vinculado a cash_shifts(id); FK diferida para orden de creación
    ticket_number VARCHAR(30) NOT NULL,
    order_status VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (
        order_status IN ('DRAFT', 'WAITING', 'IN_PREPARATION', 'READY', 'DELIVERED', 'PAID', 'CANCELLED')
    ),
    payment_method VARCHAR(20) CHECK (payment_method IN ('CASH', 'CARD', 'QR', 'TRANSFER')),
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    client_transaction_id UUID NOT NULL UNIQUE,
    notes TEXT,
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_tenant_branch_status ON orders(tenant_id, branch_id, order_status);
CREATE INDEX idx_orders_client_tx ON orders(client_transaction_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    notes TEXT
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_tenant_branch ON order_items(tenant_id, branch_id);
```

---

### E. Módulo Finanzas y Arqueo de Caja Chica

```sql
CREATE TABLE cash_shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    opened_by UUID NOT NULL REFERENCES app_users(id),
    closed_by UUID REFERENCES app_users(id),
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    initial_cash NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_cash_sales NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_card_sales NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_other_sales NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_cash_expenses NUMERIC(12, 2) NOT NULL DEFAULT 0,
    expected_cash NUMERIC(12, 2), -- initial_cash + total_cash_sales - total_cash_expenses (APPROVED)
    actual_cash NUMERIC(12, 2),
    difference NUMERIC(12, 2),
    status VARCHAR(15) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED'))
);
CREATE INDEX idx_shifts_tenant_branch ON cash_shifts(tenant_id, branch_id, status);

-- Agregar la clave foránea diferida de orders hacia cash_shifts
ALTER TABLE orders ADD CONSTRAINT fk_orders_cash_shift FOREIGN KEY (cash_shift_id) REFERENCES cash_shifts(id) ON DELETE SET NULL;
CREATE INDEX idx_orders_cash_shift ON orders(cash_shift_id);

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    cash_shift_id UUID REFERENCES cash_shifts(id) ON DELETE SET NULL,
    expense_type VARCHAR(30) NOT NULL CHECK (expense_type IN ('DAILY_OPERATIONAL', 'MONTHLY_FIXED_PRORATED')),
    description TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    paid_from_cash_drawer BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' 
        CHECK (approval_status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED')),
    approved_by UUID REFERENCES app_users(id),
    evidence_url TEXT, -- Enlace o URI del recibo o factura adjunta
    approval_notes TEXT,
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_expenses_tenant_branch_date ON expenses(tenant_id, branch_id, expense_date);
CREATE INDEX idx_expenses_shift_approval ON expenses(cash_shift_id, approval_status);
```

---

### F. Módulo de Auditoría Inmutable

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE SET NULL,
    user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_tenant_entity ON audit_logs(tenant_id, entity_name, entity_id);
```

---

## 4. Nota Técnica sobre Publicación de Eventos en Spring Modulith

Spring Modulith proporciona una tabla técnica de *Transactional Outbox* para la persistencia y garantía de entrega asíncrona de eventos de módulo (`@ApplicationModuleListener`):

- **Modo por defecto de Modulith:** Spring Modulith puede crear y gestionar su esquema automáticamente mediante `spring.modulith.events.jdbc.schema-initialization.enabled=true`.
- **Estrategia en producción con Flyway:** Para mantener el control total del versionado sin depender de la inicialización mágica en runtime, esta tabla técnica se gestiona en una migración separada e identificable: `V2__spring_modulith_outbox.sql`.
- **Estructura oficial para PostgreSQL:**
  ```sql
  CREATE TABLE event_publication (
      id UUID PRIMARY KEY,
      listener_id VARCHAR(512) NOT NULL,
      event_type VARCHAR(512) NOT NULL,
      serialized_event TEXT NOT NULL,
      publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
      completion_date TIMESTAMP WITH TIME ZONE
  );
  CREATE INDEX idx_event_publication_date ON event_publication(publication_date);
  ```