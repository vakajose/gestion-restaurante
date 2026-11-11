-- =========================================================================
-- V15: Prefijo de Categoría (code_prefix) y Unicidad por Tenant (PB-048)
-- =========================================================================

-- 1. Agregar columna code_prefix
ALTER TABLE categories ADD COLUMN IF NOT EXISTS code_prefix VARCHAR(10) DEFAULT 'CAT';

-- 2. Poblar prefijos existentes derivando las 3 primeras letras mayúsculas de su nombre
UPDATE categories 
SET code_prefix = UPPER(SUBSTRING(REGEXP_REPLACE(name, '[^a-zA-Z0-9]', '', 'g') FROM 1 FOR 3))
WHERE code_prefix IS NULL OR code_prefix = '' OR code_prefix = 'CAT';

-- Fallback para categorías con nombres muy cortos o especiales
UPDATE categories SET code_prefix = 'CAT' WHERE code_prefix IS NULL OR code_prefix = '';

-- 3. Fijar NOT NULL y restricción de unicidad por tenant
ALTER TABLE categories ALTER COLUMN code_prefix SET NOT NULL;
ALTER TABLE categories ALTER COLUMN code_prefix SET DEFAULT 'CAT';
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_categories_tenant_prefix;
ALTER TABLE categories ADD CONSTRAINT uq_categories_tenant_prefix UNIQUE (tenant_id, code_prefix);
