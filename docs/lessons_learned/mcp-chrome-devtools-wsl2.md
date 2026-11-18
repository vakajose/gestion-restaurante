---
title: "Lección Aprendida: Integración de Chrome DevTools MCP, WSL2 y Playwright"
category: "lessons_learned"
tags:
  - lessons-learned
  - mcp
  - devtools
  - wsl2
  - caddy
  - playwright
last_updated: "2026-11-17"
status: "active"
---

# Lección Aprendida: Integración de Chrome DevTools MCP con WSL2 (Fedora) y Windows

- **Actualizado:** 2026-11-17 *(Original: 2026-10-24)*  
- **Entorno:** Windows 11 + WSL2 (Fedora)  
- **Stack Local Nativo (`dev`):** Backend Spring Boot (`:8080`), ERP Angular 22 (`:4200`), Backoffice React 19 (`backoffice-frontend/` `:5173`), PostgreSQL 16 (Docker `:5432`)  
- **Stack Dockerizado (`testing-local` con Caddy):** `http://app.localhost` (ERP), `http://admin.localhost` (Backoffice), `http://api.localhost` (Backend + `/uploads/*`), `http://logs.localhost` (Dozzle), `http://localhost` (fallback single-origin), PostgreSQL (`:5432`)  
- **Herramientas:** Google Antigravity (`chrome-devtools` MCP), `.mcp/config.json` (`@modelcontextprotocol/server-puppeteer` vía CDP `:9222`), y **Playwright POM** (`frontend/e2e/` + `./scripts/run-e2e.sh`)

---

## 1. Contexto y Objetivo

Se requería que el agente de IA dentro de **Antigravity** (ejecutándose en WSL2 Fedora) pudiera inspeccionar en vivo el DOM, consola, red y comportamiento visual de **ERP Angular (`:4200` / `http://app.localhost`)** y **Backoffice React (`:5173` / `http://admin.localhost`)** usando **Google Chrome** en el host Windows mediante **Chrome DevTools Protocol (CDP)**, complementando las pruebas automatizadas deterministas en **Playwright (`frontend/e2e/`)**.

### Separación de Herramientas en el Proyecto
| Herramienta | Propósito | Alcance Recomendado |
| :--- | :--- | :--- |
| **`chrome-devtools` MCP / CDP `:9222`** | Depuración interactiva visual, UAT exploratoria en vivo, inspección de red/consola, CSS y cookies contra Chrome real en Windows | Validación manual asistida sobre `dev` (`:4200`, `:5173`) o `testing-local` (`app.localhost`, `admin.localhost`) |
| **Playwright POM (`frontend/e2e/`)** | Regresión automatizada Nivel 4 (`ADR-0017`) sin esperas rígidas ni dependencias de sesión manual | Ejecución headless/headed mediante `./scripts/run-e2e.sh` (ver guía en [`e2e-testing-guide.md`](../testing/e2e-testing-guide.md)) |

---

## 2. Obstáculos Detectados y Diagnóstico

1. **Red Tradicional NAT en WSL2:**
   - WSL2 opera por defecto bajo un conmutador virtual (*vEthernet*) con IP dinámica (`172.x.x.x`).
   - Los comandos dirigidos a `localhost:9222` dentro de WSL no alcanzaban a Windows (`Could not connect to server`), y las conexiones al gateway eran descartadas por el Firewall de Windows.

2. **Políticas de Seguridad en Google Chrome (CDP):**
   - Habilitar la casilla gráfica en `chrome://inspect/#remote-debugging` no expone el endpoint HTTP tradicional de CDP sin autenticación interna, respondiendo `HTTP ERROR 404` en `/json/version`.
   - Versiones modernas de Chrome rechazan orígenes externos si no se inicia con `--remote-allow-origins="*"` y un `--user-data-dir` dedicado.

3. **Incompatibilidad de Variables en `.mcp/config.json`:**
   - Declarar variables de entorno de Linux (`PUPPETEER_EXECUTABLE_PATH`, `DISPLAY`) simultáneamente con `PUPPETEER_BROWSER_URL` provocaba colisiones entre el Chromium de Linux y el Chrome remoto del host.
   - La configuración correcta (`.mcp/config.json`) apunta únicamente `PUPPETEER_BROWSER_URL: "http://localhost:9222"`.

4. **Resolución de Subdominios `*.localhost` en `testing-local`:**
   - En Chromium moderno bajo Windows/WSL2 con `mirrored` networking, los dominios `app.localhost`, `admin.localhost`, `api.localhost` y `logs.localhost` resuelven automáticamente a loopback (`127.0.0.1`) hacia el reverse proxy Caddy (`:80`) definido en `docker/docker-compose.testing-local.yml`.
   - Los archivos de medios (`/uploads/dishes/...`) requieren proxying explícito tanto en `http://app.localhost` como en `http://localhost` dentro de `docker/caddy/Caddyfile.testing-local` (`TASK-629`).

---

## 3. Guía Paso a Paso de Replicación

### Paso 1: Configurar Red Espejo (`Mirrored Networking`) en WSL2 (Windows)

1. Abrir PowerShell en Windows y editar la configuración global de WSL:
   ```powershell
   notepad $HOME\.wslconfig
   ```
2. Insertar:
   ```ini
   [wsl2]
   networkingMode=mirrored
   ```
3. Guardar y reiniciar WSL:
   ```powershell
   wsl --shutdown
   ```

---

### Paso 2: Regla Preventiva de Firewall en Windows

En PowerShell (Administrador):
```powershell
New-NetFirewallRule -DisplayName "WSL Chrome Debugger" -Direction Inbound -LocalPort 9222 -Protocol TCP -Action Allow
```

---

### Paso 3: Acceso Directo de Google Chrome con Perfil Aislado

1. Crear un acceso directo en Windows con destino:
   ```text
   "C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --remote-allow-origins="*" --user-data-dir="C:\ChromeMCPProfile"
   ```
2. Nombrarlo **Chrome DevTools MCP** y abrirlo antes de iniciar una sesión de depuración visual.

---

### Paso 4: Validar Conectividad CDP y Servicios desde WSL2

```bash
# Verificar CDP
curl -s http://localhost:9222/json/version

# Verificar stack dev nativo (si está levantado)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:4200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5173

# Verificar stack dockerizado testing-local (Caddy)
curl -s http://api.localhost/actuator/health
curl -s -o /dev/null -w "%{http_code}\n" http://app.localhost
curl -s -o /dev/null -w "%{http_code}\n" http://admin.localhost
```

---

### Paso 5: Configuración MCP en el Repositorio

Verificar [`.mcp/config.json`](../../.mcp/config.json) (o copiar desde [`.mcp/config.example.json`](../../.mcp/config.example.json)):

```json
{
  "mcpServers": {
    "chrome-browser": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-puppeteer"
      ],
      "env": {
        "PUPPETEER_BROWSER_URL": "http://localhost:9222"
      }
    }
  }
}
```

Además, el plugin nativo **`chrome-devtools`** de Antigravity se conecta al target activo cuando Chrome expone el puerto `9222` bajo `networkingMode=mirrored`, permitiendo inspeccionar peticiones XHR/Fetch, consola, estilos computados y capturas de pantalla sobre `app.localhost`, `admin.localhost`, `:4200` y `:5173`.

---

### Paso 6: Recarga y Ejecución

1. En el IDE, ejecutar **Developer: Reload Window** si se modificó `.mcp/config.json`.
2. Para **regresión automatizada repetible (E2E Nivel 4)** sin usar el Chrome interactivo del usuario, ejecutar Playwright POM:
   ```bash
   ./scripts/run-e2e.sh
   ```
