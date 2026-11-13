---
title: "Reporte de Ejecución de Pruebas E2E - Sprint 3 (v0.3.0)"
category: "testing"
tags:
  - testing
  - qa
  - e2e
  - sprint-3
  - regression
last_updated: "2026-10-28"
status: "verified"
---

# 🧪 Reporte de Ejecución de Pruebas E2E — Sprint 3 (v0.3.0)

Este documento registra la ejecución formal de la batería de pruebas automatizada de extremo a extremo (E2E) para certificar las funcionalidades del **Sprint 3 (Parametrización, Experiencia y Flujo de Negocio)** antes de su integración a producción (`main`).

---

## 1. Entorno y Parámetros de Ejecución

- **Fecha y Hora:** 2026-10-29 00:26:00 GMT-4
- **Entorno Host:** WSL2 sobre Linux Fedora (Windows 11 con Mirrored Networking)
- **Servidor Backend:** Spring Boot 4.1.x / Java 25 (`http://localhost:8080`)
- **Servidor Frontend:** Angular 22 Zoneless / Vite / TailwindAdmin (`http://localhost:4200`)
- **Base de Datos:** PostgreSQL 16 Alpine en Docker (`localhost:5432 / restaurant_db`) con migraciones Flyway V1 a V8 aplicadas
- **Navegador Conducido:** Google Chrome 156 (Windows) conectado vía Chrome DevTools Protocol (`http://localhost:9222`)
- **Script Ejecutable:** `scripts/e2e/run_sprint3_e2e_battery.js`

---

## 2. Matriz de Resultados por Escenario

| ID Caso | Feature / Módulo | Descripción de la Prueba | Resultado | Observaciones |
| :--- | :--- | :--- | :---: | :--- |
| **TC-ONB-001** | `PB-001` (Onboarding) | Wizard de 3 pasos, verificación de slug reactivo, coordenadas La Paz, perfil A la Carta y alta atómica de empresa y sucursal. | ✅ **APROBADO** | Aprovisionamiento atómico verificado; sesión iniciada automáticamente. |
| **TC-TOUR-001** | `PB-040` (Product Tour) | Despliegue del tour guiado interactivo con Driver.js / Signals tras completar el onboarding. | ✅ **APROBADO** | Modal de bienvenida visible, avance y salto de tour operativos. |
| **TC-CFG-001** | `PB-026` (Parametría) | Búsqueda reactiva en `/settings/parameters`, edición de parámetro y guardado en caliente (Hot-Reload) con toast. | ✅ **APROBADO** | `security.session_inactivity_timeout_minutes` actualizado y persistido sin reiniciar el servidor. |
| **TC-RBAC-001** | `PB-039` (Roles Dinámicos) | Creación de rol personalizado en `/users/roles` y asignación de permisos granulares por módulo. | ✅ **APROBADO** | Rol creado y verificado en tablas relacionales `roles` y `role_permissions`. |
| **TC-MOB-001** | `PB-028` (POS Móvil) | Emulación smartphone (390x844), modo lista compacta, barra flotante inferior y despliegue de Bottom Sheet de comanda. | ✅ **APROBADO** | Comanda accesible con una mano; swipe-up y animación GPU fluidas. |
| **TC-MOB-002** | `PB-028` (Inventario Móvil) | Ocultamiento de tabla densa en `< 768px` y despliegue de tarjetas apilables con acordeón expandible. | ✅ **APROBADO** | Bordes semánticos de color y detalle financiero expandible sin scroll horizontal. |
| **TC-AUTH-005** | `PB-027` (Sesiones / Refresh) | Login calificado por tenant, almacenamiento de tokens duales y rotación atómica en `POST /api/v1/auth/refresh`. | ✅ **APROBADO** | HTTP 200 OK; nuevo Access Token y nuevo Refresh Token emitidos. |

---

## 3. Instrucciones de Re-Ejecución

Para ejecutar nuevamente esta batería de pruebas en cualquier momento:

```bash
# Terminal 1: Iniciar Google Chrome en Windows con puerto de depuración:
chrome.exe --remote-debugging-port=9222 --remote-allow-origins="*" --user-data-dir="C:\ChromeMCPProfile"

# Terminal 2: Iniciar Backend en WSL2:
cd backend && ./mvnw spring-boot:run

# Terminal 3: Iniciar Frontend en WSL2:
cd frontend && npm start

# Terminal 4: Disparar la batería de pruebas:
cd frontend && NODE_PATH=./node_modules node ../scripts/e2e/run_sprint3_e2e_battery.js
```
