-- V16: Incorporar teléfono de contacto a colaboradores (app_users)
-- ADR-0024 & PB-045: Perfil de usuario autoservicio y humanización de colaboradores

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30);
