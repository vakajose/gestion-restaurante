-- TASK-624: Email opcional en platform_admins y política configurable de contraseñas de plataforma
ALTER TABLE platform_admins
    ALTER COLUMN email DROP NOT NULL;

DROP INDEX IF EXISTS uq_platform_admin_email;

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_admin_email
    ON platform_admins (LOWER(TRIM(email)))
    WHERE email IS NOT NULL AND TRIM(email) <> '';

INSERT INTO platform_settings (setting_key, setting_value, value_type, description)
VALUES
    (
        'security.platform_password_min_length',
        '12',
        'NUMBER',
        'Longitud mínima de contraseña para administradores de plataforma (entre 6 y 64 caracteres)'
    ),
    (
        'security.platform_password_require_complexity',
        'false',
        'BOOLEAN',
        'Exigir al menos una letra mayúscula, un dígito y un símbolo en contraseñas de plataforma'
    )
ON CONFLICT (setting_key) DO NOTHING;
