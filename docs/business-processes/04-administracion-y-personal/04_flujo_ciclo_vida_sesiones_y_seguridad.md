---
title: "04. Flujo de Ciclo de Vida de Sesiones, Seguridad de Terminales y Cierre por Inactividad"
category: "business-process"
area: "administracion-y-personal"
tags:
  - manual-operativo
  - seguridad
  - sesiones
  - cajeros
  - inactividad
  - pymes
last_updated: "2026-10-28"
status: "verified"
---

# 🛡️ 04. Flujo de Ciclo de Vida de Sesiones, Seguridad de Terminales y Cierre por Inactividad

Este manual operativo describe cómo el sistema protege la información comercial del restaurante, evita accesos indebidos a cajas desatendidas y garantiza que el personal trabaje con total comodidad sin cierres inesperados de pantalla mientras atiende a los clientes.

---

## 1. El Dilema Operativo del Restaurante: Comodidad vs. Seguridad

En el día a día de una cocina, pizzería o cafetería, los terminales de cobro sufren una paradoja habitual:
- **Si el sistema exige iniciar sesión a cada momento:** El cajero se frustra, la fila de clientes se retrasa y el ritmo de atención se rompe.
- **Si la sesión nunca expira:** La caja registradora queda abierta cuando el cajero va al baño o a la bodega, permitiendo que cualquier persona registre comandas erróneas, cobre sin permiso o anule pagos sin supervisión.

### La Solución Inteligente del Sistema:
1. **Renovación Invisible mientras trabajas:** Mientras el cajero, cocinero o administrador esté utilizando el sistema, la sesión se renueva automáticamente en segundo plano (*Silent Refresh*). El usuario nunca verá una pantalla de bloqueo a mitad de una orden.
2. **Cierre Automático si la pantalla queda sola:** Si el puesto queda abandonado por más del tiempo establecido, el sistema protege la caja: primero avisa discretamente y, si nadie responde, cierra la sesión.

---

## 2. Configuración de Tiempos según el Tipo de Terminal

Cada área del restaurante tiene ritmos distintos. Mediante la consola de parámetros del sistema ([ADR-0008](../../../adr/0008_unified_multilevel_parameter_system.md)), el dueño del restaurante puede fijar tiempos diferenciados:

| Puesto / Área | Tiempo de Inactividad Recomendado | Justificación Operativa |
| :--- | :--- | :--- |
| **Caja Mostrador / POS** | **120 minutos (2 horas)** | Permite que durante las horas valle (sin clientes en el mostrador) la pantalla siga lista para cobrar de inmediato cuando entre un pedido. |
| **Oficina / Gerencia** | **30 minutos** | Protege reportes confidenciales de ventas, utilidades y fórmulas de costos si el gerente se retira a supervisar el salón. |
| **Terminal de Cocina (KDS)** | **Ilimitado o 8 horas** | La pantalla de comandas de despacho no debe apagarse durante el turno de cocción. |

---

## 3. Protocolo Operativo Paso a Paso

### Paso 1: Inicio de Jornada y Acceso Seguro
1. El cajero ingresa a la aplicación con su nombre de usuario y contraseña personal.
2. El sistema valida su turno y genera una llave segura de operación.
3. En la barra superior, el colaborador puede ver claramente su nombre y la sucursal activa.

### Paso 2: Operación Continua sin Interrupciones
1. El colaborador registra ventas, ingresa gastos menores o consulta el inventario.
2. Cada vez que interactúa con la pantalla táctil o el teclado, el sistema renueva su permiso de acceso en fracciones de segundo.
3. No hay recargas de página, no se borran carritos de compras ni se pierden datos en progreso.

### Paso 3: Alerta Preventiva por Desatención
1. Si nadie toca el terminal durante el período establecido menos 2 minutos, aparece una ventana amigable con un cronómetro:
   > ⚠️ **"¿Sigues ahí? Por tu seguridad, la sesión se cerrará en 120 segundos."**
2. **Si el cajero regresa a tiempo:** Simplemente toca la pantalla o presiona el botón **"Continuar trabajando"**. El cronómetro se reinicia y la venta continúa exactamente donde la dejó.
3. **Si el cajero no regresa:** Al llegar a cero, el sistema bloquea la pantalla y regresa al inicio de sesión. La gaveta y los datos quedan completamente a salvo.

### Paso 4: Cierre de Turno y Salida
1. Al terminar su turno, el colaborador realiza su arqueo de caja físico ([Flujo 01 de Caja](../02-caja-y-finanzas/01_flujo_jornada_y_arqueo_caja.md)).
2. Presiona su foto de perfil y selecciona **"Cerrar Sesión"**.
3. El sistema invalida su llave de forma instantánea. El siguiente cajero deberá ingresar con sus propias credenciales para que los números queden correctamente asignados.

---

## 4. Botón de Pánico: Cierre Remoto de Sesiones

Si un empleado pierde su teléfono celular donde tenía instalada la aplicación, o si un colaborador es desvinculado de la empresa:
1. El Administrador ingresa al menú **Usuarios y Colaboradores**.
2. Selecciona al empleado y presiona **"Revocar todas las sesiones"**.
3. En un lapso máximo de 15 minutos, **todos los dispositivos vinculados a esa cuenta serán expulsados inmediatamente**, bloqueando cualquier intento de fraude o espionaje comercial.

---

## 5. Preguntas Frecuentes del Personal (FAQ)

- **¿Qué pasa si se corta el Internet mientras estoy cobrando?**  
  Gracias a la tecnología Offline-First ([ADR-0003](../../../adr/0003_offline_first_dexie_sync.md)), el POS guarda las ventas en la memoria local del equipo (IndexedDB). Cuando regrese la conexión, el sistema renovará la sesión y sincronizará los cobros automáticamente.

- **¿Puedo tener mi cuenta abierta en mi teléfono y en la computadora de la oficina al mismo tiempo?**  
  Sí. El sistema soporta sesiones simultáneas en distintos dispositivos, identificando cada uno por su navegador y dirección de red.

- **¿Por qué mi sesión se cerró sola si no toqué nada?**  
  Por la política de inactividad de tu restaurante. Si necesitas más tiempo antes de que se cierre sola, pídele a tu administrador que aumente los minutos de inactividad para tu sucursal.
