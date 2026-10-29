---
title: "Sprint 6 Planning: Infraestructura Cloud, Ambientes y SaaS Superadmin Backoffice"
category: "sprints"
sprint: 6
status: "planned"
last_updated: "2026-10-29"
---

# ☁️ Sprint 6 Planning — Infraestructura Cloud, Ambientes y SaaS Superadmin Backoffice

> **Estado del Sprint:** `PLANIFICADO / INFRAESTRUCTURA Y PLATAFORMA SAAS`  
> **Objetivo del Sprint:** Servir el producto en la nube en un entorno Staging accesible públicamente para demos comerciales y pruebas remotas. Establecer la arquitectura formal de múltiples ambientes (Desarrollo, Testing, Staging y Producción), contenerización Docker completa, almacenamiento desacoplado de medios en Object Storage compatible con S3 (PB-035), y la consola desacoplada de Superadmin SaaS / Árbitro del Sistema (PB-002 + PB-003) para aprovisionamiento, monitoreo y suspensión de tenants.

---

## 🎯 Entregable Clave del Sprint 6

- **Producto Servido en la Nube (Staging Cloud):** Instancia funcional desplegada vía Docker Compose / VPS con SSL automático (Caddy/Traefik) y base de datos gestionada.
- **Backoffice SaaS Desacoplado:** SPA independiente para el equipo fundador con autenticación supra-tenant para administrar restaurantes inquilinos.
- **Pipeline de Ambientes CI/CD:** Scripts reproducibles para levantar Dev (local), Test (CI automatizado) y Staging (Cloud).

---

## 📋 Tareas Planificadas del Sprint 6

### 1. Ambientes, Contenerización y Despliegue Cloud (PB-025)

- [ ] **TASK-601 [DEVOPS] [INFRA]:** Matriz de Ambientes (`dev`, `test`, `staging`, `prod`) y Configuración Externa Twelve-Factor (PB-025).
  - *Objetivo:* Perfiles Spring (`application-staging.yml`, `application-prod.yml`), variables de entorno para Angular y configuración de dominios/CORS.

- [ ] **TASK-602 [DEVOPS] [INFRA]:** Dockerfiles Multi-Stage Optimizados (Spring Boot 4 Java 25 + Angular Nginx) y Docker Compose de Producción (PB-025).
  - *Objetivo:* Contenedores ligeros y seguros sin privilegios root, optimizados para mínimo consumo de memoria.

- [ ] **TASK-603 [DEVOPS] [INFRA]:** Despliegue en la Nube de Entorno Staging con Certificados SSL Automáticos (PB-025).
  - *Objetivo:* Servir la aplicación en un servidor en la nube (VPS / Cloud) accesible por URL pública con HTTPS.

---

### 2. Almacenamiento Desacoplado de Medios y Optimización WebP (PB-035)

- [ ] **TASK-604 [STORAGE] [ARQUITECTURA]:** Motor de Almacenamiento Agnóstico y Cliente S3 / Cloudflare R2 (PB-035).
  - *Objetivo:* Implementar `MediaStorageService` en `core.storage` con pipeline de conversión a WebP y almacenamiento sin estado para fotos y evidencias de caja.

---

### 3. Suite SaaS Superadmin & Backoffice Desacoplado (PB-002 / PB-003)

- [ ] **TASK-605 [GLOBAL] [SEGURIDAD]:** Autenticación Supra-Tenant, Endpoints de Telemetría y Gestión de Inquilinos en Backend (PB-002).
  - *Objetivo:* Rol `SUPERADMIN` con capacidad de crear, suspender, desbloquear tenants y consultar métricas agregadas de plataforma.

- [ ] **TASK-606 [GLOBAL] [ARQUITECTURA]:** Aplicación Web Independiente para Backoffice de Superadmin (PB-003).
  - *Objetivo:* SPA desacoplada de la app de restaurante para el control operativo, facturación SaaS y soporte a clientes.
