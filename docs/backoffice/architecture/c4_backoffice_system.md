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
last_updated: "2026-11-12"
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
        BackofficeReact["backoffice-frontend/ (React LTS + Tailwind)"]
        ErpAngular["frontend/ (Angular 22 + Dexie.js)"]
    end

    subgraph Backend["backend/ (Spring Boot 4.1 + Spring Modulith)"]
        ChainPlatform["Seguridad @Order(1) JWT aud=platform-backoffice"]
        ModBackoffice["modules.backoffice (SaaS Control Plane)"]
        CoreEntitlements["core.entitlements (SPI + Cache O(1))"]
        CoreStorage["core.storage (Strategy Local / S3 OCI)"]
        ErpModules["Modulos ERP (security, company, catalog, inventory, finance, pos)"]
    end

    DB["PostgreSQL 16 (Tablas Plataforma V17-V19 + Tablas ERP)"]
    ObjStorage["OCI Object Storage / MinIO S3"]

    SuperAdmin -->|"HTTPS"| BackofficeReact
    TenantUser -->|"HTTPS"| ErpAngular
    BackofficeReact -->|"/api/v1/backoffice/**"| ChainPlatform
    ChainPlatform --> ModBackoffice
    ErpAngular -->|"/api/v1/**"| ErpModules
    ErpModules -->|"Check Acceso & Cuotas"| CoreEntitlements
    ModBackoffice -->|"Implementa SPI + Eventos"| CoreEntitlements
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
    subgraph Core["core (OPEN)"]
        Sec["core.security (Login & JwtFilter)"]
        EntApi["core.entitlements.api (TenantEntitlementService)"]
        ConfigApi["core.config.api (PlatformSettingsService)"]
        StorageApi["core.storage.api (MediaStorageService)"]
    end

    subgraph Business["Modulos de Negocio"]
        Company["modules.company"]
        Catalog["modules.catalog"]
        Inventory["modules.inventory"]
        Finance["modules.finance (Turnos Caja)"]
        POS["modules.pos (Ordenes y Ventas)"]
    end

    subgraph ControlPlane["modules.backoffice"]
        SubSvc["Subscription & Quota Service"]
        PaySvc["Charges & Payments Ledger"]
        TelListener["TelemetryEventListener (@ApplicationModuleListener)"]
        ObsCtrl["Observability & Loggers Controller"]
    end

    Sec --> EntApi
    Company --> EntApi
    Catalog --> EntApi
    Inventory --> EntApi
    Finance --> EntApi
    POS --> EntApi

    SubSvc -->|"Publica TenantEntitlementChangedEvent"| EntApi
    POS -->|"OrderPaidEvent / OrderCancelledEvent"| TelListener
    SubSvc -->|"Consulta uso por .api"| Company
    SubSvc -->|"Consulta uso por .api"| Catalog
    SubSvc -->|"Consulta uso por .api"| Inventory
    ObsCtrl --> ConfigApi
    ObsCtrl --> StorageApi
```
