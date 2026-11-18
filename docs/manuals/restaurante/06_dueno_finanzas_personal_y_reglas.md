---
title: "Guía del Dueño: Finanzas, Personal, Roles y Reglas del Sistema"
category: "end-user-manual"
module: "erp"
tags:
  - manual
  - finanzas
  - personal
  - roles
  - parametros
  - perfil
  - dueno
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 🏛️ Guía del Dueño: Finanzas, Personal, Roles y Reglas

> ⬅️ **Índice:** [Manuales del Restaurante](./README.md) | [Control de Caja, Gastos y Arqueo](./04_control_de_caja_gastos_y_arqueo.md) | [Insumos, Recetas y Kardex](./05_insumos_recetas_y_kardex.md)

Manual de referencia para dueños y administradores generales sobre auditoría histórica financiera, sucursales, gestión de equipo y permisos, seguridad personal y las 20 reglas configurables de operación.

---

## 1. Auditoría Histórica y Exportación (`Finanzas y Arqueos`)

Mientras `Control de Caja` muestra el turno en vivo, **`Finanzas y Arqueos`** es tu centro histórico para auditar cierres de días anteriores o de distintas sucursales:

1. **Filtros por Fecha y Criterios:** Usa los atajos rápidos (`Hoy`, `Esta Semana`, `Este Mes`) o un rango personalizado de fechas, y filtra por sucursal, cajero o estado del turno.
2. **Inspección Completa (`Ver Detalle`):** Abre la ficha del turno mostrando el desglose de ventas por medio de pago (`Efectivo`, `Tarjeta`, `Pago QR`, `Transferencia`), gastos aprobados, pedidos anulados y la diferencia exacta del arqueo (`Sobrante` o `Faltante`).
3. **`Consolidar` y `Reabrir`:**
   - **`Consolidar`**: sella el turno como auditado y aprobado contablemente.
   - **`Reabrir`**: permite al administrador devolver un turno consolidado si detectó un gasto mal clasificado o requiere revisión adicional.
4. **Exportar a Excel/CSV:** Descarga el resumen consolidado de turnos o el detalle completo en archivo `.csv` para tu contabilidad externa.

---

## 2. Sucursales y Datos Fiscales (`Empresa y Sucursales`)

- Actualiza el nombre comercial o **NIT / Identificación Fiscal** de la empresa.
- Crea nuevas sucursales (respetando el límite de tu plan SaaS) o edita su dirección, coordenadas, estado activo y **Huso Horario IANA** (por ejemplo `America/La_Paz`). Todas las ventas, turnos y movimientos de esa sucursal respetarán su zona horaria local.

---

## 3. Equipo de Trabajo: `Gestión de Personal` vs `Roles y Privilegios`

El sistema separa el alta diaria ágil de la administración fina de seguridad en dos pantallas:

### A. `Gestión de Personal` (Operación rápida del día a día)
Úsala cuando solo necesitas crear o editar rápidamente un usuario con uno de los **4 roles base** del sistema:
1. **Administrador de Empresa** *(acceso total a empresa, finanzas, reglas y sucursales)*.
2. **Administrador de Sucursal** *(encargado de caja, inventario, catálogo y aprobaciones de su local)*.
3. **Cajero / Punto de Venta** *(operación de ventas en POS y su turno en Control de Caja)*.
4. **Personal de Cocina** *(rol heredado previsto para futuras pantallas dedicadas de cocina)*.

En esta pantalla también puedes activar/desactivar rápidamente un usuario y marcar si debe cambiar su contraseña obligatoriamente en su próximo ingreso. *(Si la regla de correo obligatorio está activa en parámetros, el campo de correo será exigido al guardar).*

### B. `Roles y Privilegios` (Seguridad avanzada, multi-sucursal y contraseñas)
Ingresa aquí cuando necesites mayor control sobre tu equipo:
- **Crear Roles Personalizados:** Define cargos a medida (ej. *"Sub-encargado de Inventario"* o *"Auditor Contable"*) marcando exactamente qué permisos granulares tiene habilitados.
- **Asignar Múltiples Sucursales y Vigencia Temporal:** Permite que un encargado supervise 2 o más sucursales a la vez, o define **fechas de inicio y fin de vigencia** (ideal para reemplazos o personal temporal).
- **Restablecer Contraseña Temporal:** Si un empleado olvidó su clave, genera desde aquí una contraseña temporal; esto **cierra automáticamente sus sesiones activas** y lo obliga a crear su contraseña nueva al volver a entrar.

---

## 4. Seguridad Personal (`Mi Perfil`)

Tocando tu nombre en la parte inferior del menú entras a **`Mi Perfil`**, disponible para todo usuario:
- **Actualizar tus datos y cambiar tu propia contraseña.**
- **Configurar tu PIN de Supervisor (4 a 6 dígitos):** Necesario si eres dueño o encargado para autorizar anulaciones de pedidos directamente frente al cajero sin revelar tu contraseña.
- **Sesiones Activas:** Lista los dispositivos o navegadores donde tienes sesión iniciada y te permite cerrar sesiones antiguas o remotas con un clic.

---

## 5. Las 20 Reglas en `Parámetros del Sistema`

En **`Parámetros del Sistema`** ajustas el comportamiento del restaurante organizado por pestañas (`Todos`, `Punto de Venta`, `Inventario`, `Finanzas`, `Seguridad`, `General`).

- **Niveles de Seguridad:** Los cambios marcados como **`✓ Estándar`** o **`⚠️ Media`** se guardan de inmediato. Los cambios críticos marcados como **`🔒 Alta`** solicitan confirmar tu **contraseña actual** y escribir un **motivo de al menos 10 caracteres** para dejar rastro en auditoría.
- **Alcance:** Algunas reglas aplican a **toda la empresa** y otras pueden variar **por cada sucursal**.

| Categoría | Regla en Pantalla | Estado Real Hoy | Cómo impacta en tu operación |
| :--- | :--- | :---: | :--- |
| **Inventario** | **Modo de descuento de inventario** *(Empresa · Alta)* | ✅ Activo | Elige entre descuento automático permisivo, bloqueo estricto sin stock o control manual aislado. |
| **Inventario** | **Permitir stock negativo** *(Empresa · Alta)* | ✅ Activo | Decide si las existencias pueden bajar de cero al vender sin carga previa. |
| **Punto de Venta** | **Mostrar imágenes de platos en POS** *(Sucursal · Estándar)* | ✅ Activo | Vista inicial con fotos (`Galería`) o rápida de texto (`Compacto` / `Lista`) si el equipo aún no eligió preferencia local. |
| **Punto de Venta** | **Modalidad predeterminada del POS** *(Sucursal · Estándar)* | 🔜 En conexión | Prepara la modalidad inicial sugerida; **en esta versión el POS siempre inicia en `En Sala`**. |
| **Punto de Venta** | **Habilitar opción Recoger programado** *(Sucursal · Estándar)* | 🔜 En conexión | Prevista para mostrar/ocultar `Recoger`; **hoy el botón `Recoger` siempre está visible en el POS**. |
| **Punto de Venta** | **Habilitar cobro de envío en Delivery** *(Sucursal · Estándar)* | 🔜 En conexión | Prevista para bloquear o habilitar tarifa de envío; **hoy el cajero siempre puede editar el envío manualmente en Delivery**. |
| **Punto de Venta** | **Tarifa predeterminada de envío Delivery** *(Sucursal · Estándar)* | 🔜 En conexión | Prevista para precargar un monto base; **hoy el envío inicia en Bs. 0,00 hasta que el cajero ingrese el monto**. |
| **Punto de Venta** | **Política de platos ya preparados al anular** *(Sucursal · Media)* | ✅ Activo *(parcial)* | Define si un plato cocinado anulado pasa a merma definitiva en Kardex (visible hoy) o a reserva reutilizable *(la pantalla visual dedicada de esa reserva llegará en futuras versiones)*. |
| **Finanzas** | **Monto máximo de gasto en caja chica** *(Sucursal · Media)* | ✅ Activo | Tope por gasto pagado con efectivo del cajón (por defecto Bs. 100,00). |
| **Finanzas** | **Exigir PIN / autorización al anular pedidos** *(Empresa · Alta)* | ✅ Activo | Hoy toda anulación requiere PIN presencial de supervisor o aprobación en cola remota. |
| **Finanzas** | **Política de apertura de turno de caja** *(Sucursal · Media)* | ✅ Activo | Permite abrir turno a todos los cajeros autorizados o solo a supervisores. |
| **Finanzas** | **Modo de concurrencia de turnos en sucursal** *(Sucursal · Alta)* | ✅ Activo | Un solo turno compartido por sucursal o turnos independientes por cada cajero. |
| **Finanzas** | **Permitir auto-aprobación de gastos propios** *(Sucursal · Media)* | ✅ Activo | Por control dual está desactivado para cajeros; el Administrador de Empresa siempre auto-aprueba sus propios gastos. |
| **Finanzas** | **Apertura automática de jornada con Toda la Carta** *(Sucursal · Alta)* | ✅ Activo | Al abrir el primer turno del día habilita de inmediato **`Toda la Carta`** si aún no se activó un Menú del Día. |
| **Seguridad** | **Exigir correo electrónico al crear personal** *(Empresa · Alta)* | ✅ Activo | Define si el correo electrónico es obligatorio o puede omitirse al registrar cajeros o cocineros. |
| **Seguridad** | **Forzar cambio de clave en altas o reseteos** *(Empresa · Alta)* | ✅ Activo | Pre-marca el cambio obligatorio de contraseña en usuarios nuevos; en reseteos de olvido siempre se exige cambio al entrar. |
| **Seguridad** | **Tiempo de inactividad de sesión (minutos)** *(Empresa · Media)* | 🔜 En conexión | Referencia administrativa guardada; **hoy el navegador cierra sesiones inactivas de forma fija a los 30 minutos** (con aviso 2 minutos antes). |
| **General** | **Moneda de operación** *(Sucursal · Media)* | ✅ Activo | Moneda registrada para la sucursal (`BOB` por defecto; las vistas muestran el símbolo `Bs.`). |
| **General** | **Zona horaria de la sucursal** *(Sucursal · Media)* | ✅ Activo | Referencia de huso horario configurada (complementa el huso horario definido en la ficha de sucursal). |
| **General** | **Alcance del catálogo de productos** *(Empresa · Alta)* | ✅ Activo | **`Aislado por sucursal`** *(predeterminado: cada local gestiona sus platos y precios)* o **`Compartido globalmente`**. |

---

## Relacionado

- [Guía de Inicio Rápido del Restaurante](./00_guia_inicio_rapido_restaurante.md)
- [Control de Caja, Gastos Menores y Arqueo](./04_control_de_caja_gastos_y_arqueo.md)
- [Flujo de Parametrización Operativa (Procesos de Negocio)](../../business-processes/04-administracion-y-personal/01_flujo_parametrizacion_operativa_empresa.md)
- [Flujo de Gestión de Roles y Privilegios (Procesos de Negocio)](../../business-processes/04-administracion-y-personal/03_flujo_gestion_roles_y_privilegios.md)
- [Flujo de Ciclo de Vida de Sesiones y Seguridad (Procesos de Negocio)](../../business-processes/04-administracion-y-personal/04_flujo_ciclo_vida_sesiones_y_seguridad.md)
