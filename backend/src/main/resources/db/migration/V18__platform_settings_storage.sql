-- ============================================================================
-- V18__platform_settings_storage.sql
-- Configuraciones globales de plataforma (separadas de system_parameter_values
-- de restaurantes) y valores iniciales de almacenamiento agnóstico (ADR-0028).
-- ============================================================================

CREATE TABLE IF NOT EXISTS platform_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' CHECK (value_type IN ('STRING', 'BOOLEAN', 'NUMBER', 'JSON')),
    description VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES platform_admins(id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO platform_settings (setting_key, setting_value, value_type, description)
VALUES
    ('storage.active_provider', 'LOCAL', 'STRING', 'Proveedor activo de almacenamiento de medios (LOCAL o S3)'),
    ('storage.max_file_size_bytes', '1048576', 'NUMBER', 'Tamaño máximo permitido por imagen subida (default 1 MB = 1048576 bytes)'),
    ('storage.public_url_prefix', '/uploads/dishes/', 'STRING', 'Prefijo público base para construir las URLs de imágenes')
ON CONFLICT (setting_key) DO NOTHING;
