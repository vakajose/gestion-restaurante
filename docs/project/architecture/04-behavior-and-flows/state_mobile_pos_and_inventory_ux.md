---
title: "Flujo de Interacción y Estados de UX Móvil: POS Táctil e Inventario en Smartphone"
category: "architecture"
level: "Behavior-Flow"
module: "ui-ux"
tags:
  - architecture
  - mermaid
  - sdd
  - state-diagram
  - sequence-diagram
  - mobile-first
  - touch-ux
  - pos
  - inventory
last_updated: "2026-10-28"
status: "verified"
---

# 📱 Flujo de Interacción y Estados de UX Móvil: POS Táctil e Inventario

Este documento modela las máquinas de estado y diagramas de interacción táctil para dispositivos móviles (`< 768px`) en el Punto de Venta (POS) y el Levantamiento Físico de Inventario, formalizando las decisiones de diseño de **ADR-0012** y **PB-028**.

---

## 1. Máquina de Estados del Punto de Venta Móvil (POS Touch Lifecycle)

```mermaid
stateDiagram-v2
    [*] --> NavegacionCatalogo : Inicialización de Vista POS (< 768px)

    state NavegacionCatalogo {
        [*] --> ModoCuadricula : Default (2 Columnas Táctiles)
        ModoCuadricula --> ModoListaCompacta : Toca toggle "☰ Lista"
        ModoListaCompacta --> ModoCuadricula : Toca toggle "⊞ Cuadrícula"
        
        ModoCuadricula --> FiltradoCategoria : Scroll horizontal / Selección de Pill
        ModoListaCompacta --> FiltradoCategoria : Scroll horizontal / Selección de Pill
        FiltradoCategoria --> ModoCuadricula : Muestra platos de categoría
    }

    NavegacionCatalogo --> AgregarItem : Tap en botón [+] o Tarjeta de Plato
    
    state AgregarItem {
        [*] --> VerificarModificadores : Plato con extras o combos
        VerificarModificadores --> ModalModificadoresBottomSheet : Requiere selección (término, salsa, tamaño)
        VerificarModificadores --> PersistirEnCarrito : Plato simple sin modificadores obligatorios
        ModalModificadoresBottomSheet --> PersistirEnCarrito : Confirma selección de modificadores
    }

    AgregarItem --> CarritoActualizado : Incrementa Signals y actualiza Dexie.js

    state CarritoActualizado {
        [*] --> BarraFlotanteInferiorVisible : Comanda tiene >= 1 ítem
        note right of BarraFlotanteInferiorVisible
            Muestra badge flotante fijo al pie:
            "🛒 N ítems | Bs. Total [Ver Comanda ⬆]"
        end note
    }

    BarraFlotanteInferiorVisible --> ComandaBottomSheetAbierta : Tap o Swipe-up en Barra Flotante

    state ComandaBottomSheetAbierta {
        [*] --> InspeccionItems
        InspeccionItems --> ModificarCantidad : Tap en [+] / [-]
        InspeccionItems --> EliminarItem : Tap en [🗑️]
        InspeccionItems --> AgregarNotaCocina : Tap en "Nota" (Ej: "Sin cebolla")
        InspeccionItems --> VaciarComanda : Tap en "Vaciar Todo" (con confirmación)
    }

    ComandaBottomSheetAbierta --> BarraFlotanteInferiorVisible : Swipe-down o tap en backdrop / [✕ Cerrar]
    ComandaBottomSheetAbierta --> CheckoutBottomSheetAbierto : Tap en [💰 Proceder al Cobro]

    state CheckoutBottomSheetAbierto {
        [*] --> SeleccionMetodoPago
        SeleccionMetodoPago --> PagoEfectivo : Tap en "Efectivo"
        SeleccionMetodoPago --> PagoQR : Tap en "QR Rápido"
        SeleccionMetodoPago --> PagoTarjeta : Tap en "Tarjeta Débito/Crédito"

        PagoEfectivo --> NumpadVirtualTactil : Ingreso de monto recibido
        NumpadVirtualTactil --> CalculoVueltoAutomatico : Cálculo instantáneo
    }

    CheckoutBottomSheetAbierto --> ConfirmarVenta : Tap en [✅ Finalizar Venta]
    
    state ConfirmarVenta {
        [*] --> GuardarDexie : Inserta Order en IndexedDB con client_transaction_id
        GuardarDexie --> DispararSync : Si está Online envía POST /pos/orders/sync
        DispararSync --> EmisionTicket : Modal éxito con opción imprimir o enviar WhatsApp
    }

    ConfirmarVenta --> NavegacionCatalogo : Reset de Carrito a estado inicial
```

---

## 2. Diagrama de Secuencia: Despliegue de Comanda y Cobro Móvil Táctil

```mermaid
sequenceDiagram
    autonumber
    actor Operador as 👤 Mesero / Cajero Móvil
    participant UI as 📱 POS Component (Angular 22)
    participant Sheet as 📑 BottomSheet Cart Component
    participant Service as ⚡ PosService (Signals)
    participant DB as 💾 Dexie.js (IndexedDB Local)
    participant API as 🌐 Spring Modulith (/api/v1/pos)

    Note over Operador, UI: Pantalla Smartphone (< 768px)
    Operador->>UI: Tap en plato "Hamburguesa Especial" (+)
    UI->>Service: addToCart(dish, modifiers)
    Service->>Service: Actualiza signal cartItems() y totalAmount()
    Service-->>UI: Notifica cambio reactivo
    UI->>UI: Renderiza "Floating Bottom Bar" (z-index 30, sticky bottom)
    
    Operador->>UI: Tap en "🛒 1 Ítem | Bs. 45.00 [Ver Comanda ⬆]"
    UI->>Sheet: openBottomSheet({ items, totals })
    Sheet->>Sheet: Animación CSS GPU (translateY: 0%, backdrop blur)
    Sheet-->>Operador: Muestra comanda expandida ocupando 85% del viewport

    Operador->>Sheet: Tap en [+] para duplicar cantidad (Cant: 2)
    Sheet->>Service: updateQuantity(dishId, 2)
    Service-->>Sheet: Recalcula total (Bs. 90.00)

    Operador->>Sheet: Tap en [💰 Proceder al Cobro]
    Sheet->>UI: transitionToCheckoutSheet()
    UI-->>Operador: Despliega Numpad táctil y medios de pago
    Operador->>UI: Selecciona "Efectivo", digita Bs. 100.00
    UI->>UI: Computa vuelto en tiempo real: Bs. 10.00

    Operador->>UI: Tap en [✅ Confirmar Cobro]
    UI->>DB: orders.put({ client_transaction_id: UUID, items, paymentMethod: 'CASH', total: 90 })
    UI->>Service: clearCart()
    UI->>Sheet: closeBottomSheet()

    alt Conexión Online Disponible
        UI->>API: POST /api/v1/pos/orders/sync (Header X-Client-Transaction-Id)
        API-->>UI: 200 OK { orderId, status: 'PAID' }
        UI-->>Operador: Badge verde "🟢 Cobrado y Sincronizado"
    else Sin Conexión (Modo Offline)
        UI-->>Operador: Badge amarillo "🟠 Guardado Localmente en Dexie"
    end
```

---

## 3. Máquina de Estados del Inventario Táctil (Mobile Physical Stock Count)

```mermaid
stateDiagram-v2
    [*] --> ListaTarjetasInsumos : Vista Inventario en Móvil (< 768px)

    state ListaTarjetasInsumos {
        [*] --> TarjetaColapsada
        note right of TarjetaColapsada
            Muestra cabecera compacta:
            - Nombre de insumo
            - Badge de unidad (KG, LT, UNID)
            - Stock Actual destacado
            - Alerta de color (Verde/Amarillo/Rojo)
        end note

        TarjetaColapsada --> TarjetaExpandida : Tap en la tarjeta (Acordeón)
        TarjetaExpandida --> TarjetaColapsada : Tap en cabecera
        
        state TarjetaExpandida {
            [*] --> VistaDetallesCostos
            note right of VistaDetallesCostos
                Despliega:
                - Costo promedio unitario
                - Valorización total del insumo
                - Botones de acción directa
            end note
        }
    }

    TarjetaExpandida --> ModalAjusteBottomSheet : Tap en [⚙️ Ajustar Existencias]
    TarjetaExpandida --> ModalStockInicialBottomSheet : Tap en [🌱 Alta Stock Inicial]

    state ModalAjusteBottomSheet {
        [*] --> FormularioTáctil
        FormularioTáctil --> SeleccionMotivo : "MERMA" | "CONTEO_FISICO" | "CORRECCION"
        FormularioTáctil --> IngresoCantidadFisica : Teclado numérico táctil nativo
        IngresoCantidadFisica --> ConfirmarAjuste : Tap en [Guardar Ajuste]
    }

    ConfirmarAjuste --> ActualizacionReactiva : Invocación POST /api/v1/inventory/adjustments
    ActualizacionReactiva --> ListaTarjetasInsumos : Actualiza stock de la tarjeta y emite alerta de éxito
```

---

## 4. Matriz de Componentes y Pautas de Accesibilidad Táctil (WCAG 2.2)

| Componente | Rol en Interfaz | Dimensiones Táctiles Mínimas | Comportamiento Móvil (`< 768px`) |
| :--- | :--- | :--- | :--- |
| **`FloatingCartBar`** | Resumen y disparador del carrito | Altura `60px`, `w-full`, `fixed bottom-0` | Persistente cuando `cartItems().length > 0`. Safe-area padding para iOS. |
| **`BottomSheetContainer`** | Contenedor modal deslizable | Drag handle superior `40x4px`, altura dinámica `50vh` a `90vh` | Arrastre vertical (*touch drag*), scroll interno independiente, backdrop oscuro. |
| **`CatalogItemCard` (Grid)** | Tarjeta visual de producto | Min height `120px`, botón `+` de $48 \times 48\text{px}$ | Cuadrícula de 2 columnas con tipografía de precio grande. |
| **`CatalogItemListRow` (Compact)** | Fila rápida de un toque | Altura `56px`, fila completa interactiva | Nombre a la izquierda, precio al centro, botón `+` de $44 \times 44\text{px}$ a la derecha. |
| **`StockCardAccordion`** | Tarjeta de insumo en almacén | Altura colapsada `64px`, padding `p-3.5` | Borde izquierdo con indicador de color (4px), área de toque completa para expandir. |
| **`VirtualNumpad`** | Teclado táctil para cobro en POS | Botones de dígitos de $52 \times 52\text{px}$ | Teclado de 12 teclas (0-9, punto decimal, borrar) para cobro veloz con una mano. |
