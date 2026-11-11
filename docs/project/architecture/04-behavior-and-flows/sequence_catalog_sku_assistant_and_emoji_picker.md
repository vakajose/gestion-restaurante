---
title: "Diagrama de Secuencia: Asistente de SKU Secuencial y Selector de Emojis de Categoría"
category: "architecture"
level: "Sequence"
module: "catalog"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - catalog
  - sku
  - emoji
  - sprint-5
last_updated: "2026-11-08"
status: "verified"
---

# 🪄 Diagrama de Secuencia: Asistente de SKU Secuencial y Selector de Emojis (PB-048 / TASK-507)

Este documento modela las interacciones para el alta inteligente y asistida en el catálogo de productos: selección interactiva de emojis gastronómicos mediante popover nativo, sugerencia de prefijo único de categoría y generación determinista del correlativo SKU (`BEB-1`, `BEB-2`) con política `MAX + 1` en backend.

---

## 1. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador / Gerente
    participant CatModal as Modal Crear Categoría (Frontend)
    participant EmojiPicker as app-emoji-picker (Popover)
    participant DishModal as Modal Crear Plato (Frontend)
    participant Api as CatalogController (Backend)
    participant Svc as CatalogServiceImpl
    participant DB as PostgreSQL (categories / dishes)

    %% Flujo 1: Creación de Categoría con Selector de Emojis
    Note over Admin, DB: Flujo 1: Alta de Categoría con Emoji y Prefijo Único
    Admin->>CatModal: Clic en "Nueva Categoría"
    CatModal->>CatModal: Calcula sortOrder = max(sortOrders) + 1
    Admin->>CatModal: Ingresa nombre: "Bebidas Calientes"
    CatModal->>CatModal: Infiere sugerencia de prefijo: "BEB"
    Admin->>EmojiPicker: Clic en botón de icono actual [ 🍽️ ]
    EmojiPicker->>EmojiPicker: Abre popover flotante con cuadrícula gastronómica
    Admin->>EmojiPicker: Selecciona [ ☕ ]
    EmojiPicker-->>CatModal: Asigna icon = "☕"
    Admin->>CatModal: Clic en "Registrar Categoría"
    CatModal->>Api: POST /api/v1/catalog/categories (name, icon, codePrefix, sortOrder)
    Api->>Svc: createCategory(request)
    Svc->>DB: Verifica unicidad de code_prefix para el tenant
    Svc->>DB: INSERT INTO categories (name, icon, code_prefix, sort_order)
    DB-->>Svc: Categoría guardada
    Svc-->>Api: 201 Created (CategoryDto con codePrefix)
    Api-->>CatModal: Categoría registrada con éxito

    %% Flujo 2: Alta de Plato con Asistente de SKU
    Note over Admin, DB: Flujo 2: Asistente de Código SKU Secuencial
    Admin->>DishModal: Clic en "Nuevo Plato"
    Admin->>DishModal: Selecciona categoría "Bebidas Calientes" (codePrefix: "BEB")
    DishModal->>Api: GET /api/v1/catalog/dishes/next-code?categoryId={id}
    Api->>Svc: suggestNextDishCode(categoryId)
    Svc->>DB: SELECT code FROM dishes WHERE tenant_id = ... AND code LIKE 'BEB-%'
    DB-->>Svc: Códigos encontrados: ['BEB-1', 'BEB-2']
    Svc->>Svc: Calcula siguiente = max(1, 2) + 1 = 3
    Svc-->>Api: Retorna { nextCode: "BEB-3" }
    Api-->>DishModal: 200 OK ("BEB-3")
    DishModal->>DishModal: Precarga newDishCode = "BEB-3"
    
    alt Usuario acepta código sugerido
        Admin->>DishModal: Ingresa nombre, precio e insumos
    else Usuario introduce código contable propio
        Admin->>DishModal: Escribe manualmente "EAN-770123" (se respeta)
    end

    Admin->>DishModal: Clic en "Guardar Plato"
    DishModal->>Api: POST /api/v1/catalog/dishes (code: "BEB-3", ...)
    Api->>Svc: createDish(request)
    Svc->>DB: INSERT INTO dishes (...)
    DB-->>Svc: Plato persistido con código inmutable
    Svc-->>Api: 201 Created (DishDto)
```

---

## 2. Invariantes del Flujo

1. **Unicidad de Prefijo:** Ninguna categoría puede registrar un `code_prefix` ya utilizado por otra categoría del mismo tenant.
2. **Consecutivo Auditado (`MAX + 1`):** La secuencia se calcula siempre sumando 1 al número mayor registrado con ese prefijo; nunca se rellenan huecos de platos eliminados o inactivos.
3. **Formato Simple:** Los códigos autogenerados no llevan ceros a la izquierda (`BEB-1`, `BEB-2`, `HAM-15`), garantizando brevedad y legibilidad en tickets térmicos de cocina.
4. **Respeto a la Entrada Manual:** La entrada manual del usuario o lector de código de barras tiene precedencia sobre la sugerencia automática del sistema.
