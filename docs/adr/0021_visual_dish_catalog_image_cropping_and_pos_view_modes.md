# ADR-0021: Catálogo Visual con Imágenes, Recorte Nativo WebP, Selector Triple y Ergonomía POS (PB-036)

- **Estado:** `ACEPTADO`
- **Fecha:** 2026-11-08
- **Autores:** Squad Multi-Agente & Líder de Proyecto (Co-Diseño Interactivo)
- **Alcance:** Catálogo de Platos, Terminal de Punto de Venta (POS), UI/UX Responsivo, Almacenamiento Desacoplado
- **Requerimiento:** `PB-036` (Catálogo Visual con Imágenes de Platos en Punto de Venta y Toggle Parametrizable)
- **Tareas Relacionadas:** `TASK-506` [CATALOG] [UI/UX]

---

## 1. Contexto y Problemática

En terminales de punto de venta gastronómicos, la visualización fotográfica de los platos acelera sustancialmente la identificación por parte de meseros y cajeros, especialmente en cartas extensas o durante la curva de aprendizaje de personal nuevo. No obstante, plantear un catálogo con imágenes sin un diseño arquitectónico riguroso acarrea problemas críticos:
1. **Saturación de Red y Almacenamiento:** Subir imágenes crudas capturadas con cámaras de smartphones modernos (4 MB a 12 MB) satura el ancho de banda del restaurante y colapsa el almacenamiento del servidor.
2. **Deformación y Desalineación en UI:** Fotos con diferentes proporciones (cuadradas, panorámicas, verticales) rompen la cuadrícula visual de ventas, provocando saltos de scroll y una apariencia desordenada.
3. **Pérdida de Rendimiento en Hora Pico:** Cajeros experimentados o terminales táctiles de mostrador de bajo rendimiento priorizan la velocidad de digitación sobre el atractivo visual; forzar la carga de imágenes pesadas ralentiza la operación.
4. **Espacio en Pantalla Móvil:** En teléfonos móviles o tabletas de mesero, los encabezados extensos y botones sobredimensionados reducen el área útil de catálogo y comanda.

---

## 2. Decisiones Arquitectónicas

### A. Extensión del Modelo Relacional (`dishes`)
- Se incorpora la columna `image_url VARCHAR(500)` en la tabla `dishes` (migración Flyway `V14__dish_image_url.sql`).
- La entidad `Dish.java`, su DTO público `DishDto` y el modelo frontend `Dish` se actualizan para propagar este atributo.

### B. Servicio Desacoplado de Almacenamiento (`LocalFileStorageService`)
- Siguiendo el principio de infraestructura desacoplada, la ruta física no es un parámetro de tenant/sucursal, sino una configuración global del sistema (`application.yml`):
  ```yaml
  app:
    storage:
      type: local # En producción escalará a 's3' o 'minio' de forma transparente
      local-path: ${APP_STORAGE_LOCAL_PATH:/var/restaurant/uploads/dishes/}
      public-url-prefix: ${APP_STORAGE_PUBLIC_URL_PREFIX:/uploads/dishes/}
  ```
- Exposición estática en Spring Boot mediante `ResourceHandlerRegistry` mapeando `/uploads/**` al directorio físico configurado.
- Endpoint REST para subida: `POST /api/v1/catalog/dishes/{id}/image` con límite estricto de seguridad de 1 MB en backend.

### C. Componente Nativo de Recorte y Compresión Frontend (`app-image-cropper-modal`)
Aplicando la **Regla 9/10 de Gobernanza Tecnológica**, se descartan librerías externas pesadas de npm (`cropperjs`, `ngx-image-cropper`) en favor de un componente nativo puro en Angular 22 Zoneless:
- **Aspect Ratio Fijo:** **4:3 (400x300 px)**, proporción óptima para platos y bandejas de comida.
- **Interacción:** El usuario puede arrastrar la imagen (pan) para centrarla y ajustar el zoom mediante slider o rueda del ratón.
- **Compresión y Conversión en Cliente:** Al confirmar, un `<canvas>` oculto en memoria renderiza la porción visible y la exporta nativamente a WebP:
  `canvas.toBlob(blobCallback, 'image/webp', 0.80);`
- **Resultado:** Archivo ultraligero de **~25 KB a 35 KB** (reducción del 99% de ancho de banda).

### D. Estrategia de Fallback Visual Homogéneo
- Si un plato no cuenta con `image_url` o si la imagen falla al cargar (`(error)="onImgError()"`):
  - Se despliega un contenedor de exactamente la misma altura fija (`h-28` o `h-32`) con fondo degradado sutil (`bg-gradient-to-br from-lightprimary/30 to-lightgray dark:from-darkborder dark:to-dark`).
  - Se renderiza un icono/emoji temático de la categoría culinaria (ej. 🍕, 🍔, 🥗, ☕, 🍺, 🍰, 🥩) centrado y elegante.
  - La cuadrícula conserva rigurosamente su alineación horizontal sin saltos.

### E. Selector Triple de Vistas en POS (`catalogViewMode`)
Se unifican los modos de catálogo en la cabecera en un selector triple:
1. **`COMPLETO` (Galería Visual):** Tarjetas ricas con foto 4:3 o fallback temático.
2. **`COMPACTO` (Botones Rápidos):** Cuadrícula densa de botones de texto (estado actual).
3. **`LISTA` (Renglones):** Lista vertical compacta para lectura veloz por código y nombre.
- **Prevalencia:**
  - El parámetro de sucursal `pos.display_dish_images` define el modo inicial por defecto (`true` $\rightarrow$ `COMPLETO`, `false` $\rightarrow$ `COMPACTO`).
  - El cajero puede conmutar en caliente; su elección se persiste localmente en `localStorage` (`pos_catalog_view_mode_${branchId}`) sin alterar la configuración de otros terminales.

### F. Ergonomía de Barra de Herramientas y Adaptabilidad Móvil
1. **Desktop:** Contenedor unificado de herramientas en una sola línea compacta:
   `[ 🔍 Buscador de Platos (ancho reducido) | 🛵 Selector Modalidad | 🔲 Selector Triple de Vista ]`
2. **Móvil (Formato Responsivo):**
   - Se oculta el título extenso *"Punto de Venta..."* en pantallas pequeñas.
   - En ese espacio superior se coloca el selector de modalidades de servicio en formato de iconos compactos.
   - Se sustituye el icono de salón (`🍽️`) por una **Mesa** (`🪑` o SVG) para simbolizar claramente "Mesa / Salón".
   - En el modal de checkout móvil, los 4 métodos de pago (`Efectivo`, `Tarjeta`, `QR`, `Transferencia`) se alinean en una **única fila horizontal** de 4 columnas compactas.

### G. Sinergia de Caché y Modo Offline (Dexie.js)
- Dexie.js almacena el string `imageUrl` en `LocalProduct` / `cachedProducts`. No guarda binarios pesados para proteger la cuota de IndexedDB.
- El servidor emite cabeceras HTTP `Cache-Control: public, max-age=31536000, immutable`.
- En modo offline, el componente consulta `imageUrl` en Dexie, y el navegador sirve los bytes de la imagen directamente desde su **Disk Cache local**, logrando renderizado instantáneo sin internet.

---

## 3. Consecuencias y Beneficios

- **Velocidad y Ahorro:** Subidas en milisegundos gracias a la compresión cliente WebP de 30 KB.
- **Flexibilidad Operativa:** El restaurante puede operar en modo galería visual atractiva o conmutar a botones compactos de alta velocidad con un solo clic.
- **Cero Dependencias Bloatware:** Se mantiene el bundle de Angular 22 Zoneless libre de librerías externas de recorte.
- **Resiliencia Offline:** Platos e imágenes se mantienen visibles durante cortes de conectividad gracias a la sinergia Dexie + Browser Disk Cache.
