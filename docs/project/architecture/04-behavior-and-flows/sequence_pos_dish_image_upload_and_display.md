---
title: "Diagrama de Secuencia: Recorte Nativo de Imagen, Almacenamiento y Despliegue en POS"
category: "architecture"
level: "Sequence"
module: "catalog"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - catalog
  - pos
  - webp
  - sprint-5
last_updated: "2026-11-08"
status: "verified"
---

# 📸 Diagrama de Secuencia: Recorte Nativo, Almacenamiento y Despliegue en POS (PB-036 / TASK-506)

Este documento modela el ciclo de vida completo de las imágenes de platos: encuadre y compresión nativa en Angular 22 Zoneless, subida desacoplada al backend, persistencia en base de datos y despliegue reactivo en el terminal POS con soporte de caché de disco y modo offline en Dexie.js.

---

## 1. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador / Chef
    participant CatalogUI as Modal de Plato (/catalog)
    participant Cropper as app-image-cropper-modal
    participant Canvas as HTML5 Canvas (Offscreen)
    participant Api as CatalogController (Backend)
    participant Storage as LocalFileStorageService
    participant DB as PostgreSQL (dishes)
    actor Cajero as Cajero / Mesero
    participant PosUI as PosComponent (Terminal POS)
    participant Dexie as Dexie.js (IndexedDB)
    participant DiskCache as Browser Disk Cache

    %% Fase 1: Selección y Encuadre Interactivo
    Note over Admin, Canvas: Fase 1: Encuadre 4:3 y Compresión WebP en Cliente
    Admin->>CatalogUI: Selecciona foto cruda (8 MB)
    CatalogUI->>Cropper: Abre modal de encuadre con foto
    Admin->>Cropper: Ajusta posición (pan) y zoom (slider)
    Admin->>Cropper: Clic en "Confirmar Recorte"
    Cropper->>Canvas: Dibuja coordenadas visibles en 400x300 px
    Canvas->>Canvas: canvas.toBlob('image/webp', 0.80)
    Canvas-->>Cropper: Blob WebP (~30 KB)
    Cropper-->>CatalogUI: Retorna Blob optimizado

    %% Fase 2: Subida y Almacenamiento
    Note over CatalogUI, DB: Fase 2: Persistencia Desacoplada en Backend
    CatalogUI->>Api: POST /api/v1/catalog/dishes/{id}/image (Multipart)
    Api->>Api: Valida tamaño < 1 MB y MIME image/webp
    Api->>Storage: saveFile(blob, "dish_<uuid>.webp")
    Storage->>Storage: Escribe en directorio físico /uploads/dishes/
    Storage-->>Api: Retorna URL relativa "/uploads/dishes/dish_abc.webp"
    Api->>DB: UPDATE dishes SET image_url = ... WHERE id = ...
    DB-->>Api: Confirmado
    Api-->>CatalogUI: 200 OK (DishDto con imageUrl)

    %% Fase 3: Despliegue y Caché en POS
    Note over Cajero, DiskCache: Fase 3: Renderizado en POS y Caché Offline
    Cajero->>PosUI: Abre terminal de punto de venta
    PosUI->>PosUI: Evalúa pos.display_dish_images y localStorage
    PosUI->>Dexie: Sincroniza y almacena imageUrl en cachedProducts
    alt Imagen ya descargada previamente
        PosUI->>DiskCache: GET /uploads/dishes/dish_abc.webp
        DiskCache-->>PosUI: Retorna imagen desde disco local (0 ms, sin red)
    else Primera descarga con conexión
        PosUI->>Storage: GET /uploads/dishes/dish_abc.webp
        Storage-->>PosUI: Retorna bytes con Cache-Control: max-age=31536000
        PosUI->>DiskCache: Almacena bytes en caché del navegador
    else Sin imagen o fallo de carga
        PosUI->>PosUI: Renderiza fallback temático con degradado y emoji 🍕
    end
    PosUI-->>Cajero: Despliega cuadrícula con tarjetas perfectamente alineadas 4:3
```

---

## 2. Invariantes del Flujo

1. **Compresión Mandatoria en Cliente:** Ninguna imagen cruda de más de 1 MB viaja por la red; el canvas cliente garantiza que el payload hacia el backend sea siempre un archivo WebP de $\sim 25\text{--}35\text{ KB}$.
2. **Proporción Determinista:** Todas las imágenes generadas por el cropper miden exactamente **$400 \times 300\text{ px}$ (relación 4:3)**, erradicando desalineaciones en la cuadrícula.
3. **Resiliencia Offline:** La presencia de `imageUrl` en Dexie.js permite que el navegador sirva las fotos desde su caché local de disco ante caídas de conexión externa.
