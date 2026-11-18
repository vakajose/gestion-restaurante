---
title: "C4 Contenedores y Componentes — Ecosistema Backoffice SaaS"
category: "architecture"
level: "C4-Container"
module: "backoffice"
tags:
  - architecture
  - mermaid
  - c4-model
  - backoffice
  - spring-modulith
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# C4 Contenedores y Componentes — Ecosistema Backoffice SaaS

> ⬅️ [Volver al Índice Backoffice](../README.md) | [Especificación Arquitectura](../sdd/01_backoffice_architecture.md)

## 1. Diagrama C4 Nivel 2 (Contenedores Separados)

```mermaid
flowchart TB
    SuperAdmin["Operador / SuperAdmin SaaS"]
    TenantUser["Usuario Restaurante (ERP / POS)"]

    subgraph SPAs["Aplicaciones Frontend Independientes (Nginx)"]
        BackofficeReact["backoffice-frontend/ (React 19 LTS + Vite + Tailwind 3.4)"]
        ErpAngular["frontend/ (Angular 22 Zoneless + Dexie.js)"]
    end

    subgraph Backend["backend/ (Spring Boot 4.1.1 + Spring Modulith 2.1.1)"]
        ChainPlatform["BackofficeSecurityConfig @Order(1) JWT aud=platform-backoffice"]
        ModBackoffice["modules.backoffice (SaaS Control Plane)"]
        CoreEntitlements["core.entitlements (SPI + Cache O(1))"]
        CoreStorage["core.storage (MediaStorageService Local / S3)"]
        ErpModules["Modulos ERP (security, company, catalog, inventory, finance, pos)"]
    end

    DB["PostgreSQL 16 (Tablas Plataforma V17–V22 + Tablas ERP V1–V24)"]
    ObjStorage["OCI Object Storage / MinIO / S3"]

    SuperAdmin -->|"HTTPS (Caddy)"| BackofficeReact
    TenantUser -->|"HTTPS (Caddy)"| ErpAngular
    BackofficeReact -->|"/api/v1/backoffice/**"| ChainPlatform
    ChainPlatform --> ModBackoffice
    ErpAngular -->|"/api/v1/**"| ErpModules
    ErpModules -->|"Check Acceso & Cuotas"| CoreEntitlements
    ModBackoffice -->|"TenantEntitlementPortImpl + Eventos"| CoreEntitlements
    ModBackoffice -->|"Test Conexion Activo"| CoreStorage
    ErpModules -->|"Upload Media"| CoreStorage
    CoreStorage -->|"AWS SDK v2"| ObjStorage
    ModBackoffice --> DB
    ErpModules --> DB
```

---

## 2. Diagrama C4 Nivel 3 (Comunicación Event-Driven Spring Modulith)

```mermaid
flowchart LR
    subgraph Core["core (Núcleo y Contratos .api)"]
        Sec["core.security (JwtFilter + UserPublicApi + SessionPublicApi)"]
        EntApi["core.entitlements.api (TenantEntitlementService: assertPosAndShiftOperationsAllowed, getEntitlementView)"]
        ConfigApi["core.config.api (PlatformSettingsService)"]
        StorageApi["core.storage.api (MediaStorageService)"]
    end

    subgraph Business["Módulos de Negocio ERP"]
        Company["modules.company (CompanyPublicApi, BranchPublicApi, OnboardingController)"]
        Catalog["modules.catalog (CatalogPublicApi)"]
        Inventory["modules.inventory (InventoryPublicApi)"]
        Finance["modules.finance (Turnos Caja)"]
        POS["modules.pos (PosPublicApi, Ordenes)"]
    end

    subgraph ControlPlane["modules.backoffice"]
        SubSvc["TenantSubscriptionService & SubscriptionPlanService"]
        TenantSvc["BackofficeTenantService & TenantUsageSnapshotReader"]
        TelListener["BackofficeTelemetryListener (@EventListener + @ApplicationModuleListener)"]
        ObsSvc["BackofficeObservabilityService & BackofficeOperationsController"]
    end

    Sec --> EntApi
    Company --> EntApi
    Catalog --> EntApi
    Inventory --> EntApi
    Finance --> EntApi
    POS --> EntApi

    SubSvc -->|"Publica TenantEntitlementChangedEvent"| EntApi
    SubSvc -->|"Revoca sesiones via SessionPublicApi"| Sec
    POS -->|"OrderPaidEvent / OrderCancelledEvent"| TelListener
    TenantSvc -->|"UserPublicApi"| Sec
    TenantSvc -->|"CompanyPublicApi / BranchPublicApi"| Company
    TenantSvc -->|"CatalogPublicApi"| Catalog
    TenantSvc -->|"InventoryPublicApi"| Inventory
    TenantSvc -->|"PosPublicApi"| POS
    ObsSvc --> ConfigApi
    ObsSvc --> StorageApi
```
