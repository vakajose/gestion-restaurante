---
title: "Lección Aprendida: Integración de Chrome DevTools MCP con WSL2 y Windows"
category: "lessons_learned"
tags:
  - lessons-learned
  - mcp
  - devtools
  - wsl2
last_updated: "2026-10-25"
status: "verified"
---

# Lección Aprendida: Integración de Chrome DevTools MCP con WSL2 (Fedora) y Windows


- **Fecha:** 2026-10-24  
- **Entorno:** Windows 11 + WSL2 (Fedora)  
- **Stack de Prueba:** Spring Boot (Backend :8080), Angular 22 (Frontend :4200), PostgreSQL (Docker :5432)  
- **Herramientas:** Google Antigravity, Model Context Protocol (MCP), Chrome DevTools Protocol (CDP)

---

## 1. Contexto y Objetivo

Se requería que el agente de IA dentro de **Antigravity** (ejecutándose en un entorno Linux WSL2 Fedora) fuera capaz de ejecutar pruebas end-to-end (e2e) y navegación interactiva visual utilizando una ventana real de **Google Chrome** en el host de Windows, permitiéndole interactuar con el DOM, disparar eventos y auditar tráfico de red/consola mediante Chrome DevTools Protocol (CDP).

---

## 2. Obstáculos Detectados y Diagnóstico

1. **Red Tradicional NAT en WSL2:**
   - WSL2 opera por defecto bajo un conmutador virtual (*vEthernet*) con direccionamiento IP dinámico (`172.x.x.x`).
   - Los comandos dirigidos a `localhost:9222` dentro de WSL no alcanzaban a Windows (`Could not connect to server`).
   - Las conexiones directas a la IP del gateway (`$WIN_IP:9222`) eran descartadas o bloqueadas silenciosamente por el Firewall de Windows.

2. **Políticas de Seguridad en Google Chrome (CDP):**
   - Habilitar la casilla gráfica en `chrome://inspect/#remote-debugging` no expone el endpoint HTTP tradicional de CDP sin autenticación interna, respondiendo con `HTTP ERROR 404` en `/json/version`.
   - Las versiones modernas de Chrome bloquean peticiones HTTP externas por protección contra ataques CSRF/DNS Rebinding si no se especifica explícitamente `--remote-allow-origins="*"`.

3. **Incompatibilidad de Parámetros en el archivo MCP:**
   - Declarar variables de entorno de Linux (`PUPPETEER_EXECUTABLE_PATH`, `DISPLAY`) simultáneamente con `PUPPETEER_BROWSER_URL` provocaba colisiones entre el Chromium headless empaquetado de Linux y el Chrome remoto del host.
   - La solución consiste en apuntar únicamente a `PUPPETEER_BROWSER_URL: "http://localhost:9222"`.

---

## 3. Guía Paso a Paso de Replicación

### Paso 1: Configurar Red Espejo (Mirrored Networking) en WSL2 (Windows)

Para compartir exactamente el espacio de loopback (`localhost`) entre Windows y WSL2, eliminando problemas de IP dinámica y firewalls virtuales:

1. Abrir PowerShell en Windows y editar el archivo de configuración global de WSL:
   ```powershell
   notepad $HOME\.wslconfig
   ```

2. Insertar el bloque de configuración:
   ```ini
   [wsl2]
   networkingMode=mirrored
   ```

3. Guardar el archivo y reiniciar el subsistema:
   ```powershell
   wsl --shutdown
   ```

---

### Paso 2: Regla de Firewall en Windows (Opcional pero Preventivo)

Para garantizar que el puerto de depuración no sea filtrado por directivas del sistema:

En PowerShell (como Administrador):
```powershell
New-NetFirewallRule -DisplayName "WSL Chrome Debugger" -Direction Inbound -LocalPort 9222 -Protocol TCP -Action Allow
```

---

### Paso 3: Crear el Acceso Directo de Google Chrome en Windows

Para evitar abrir manualmente Chrome desde consola con flags largos cada vez, se utiliza un perfil aislado (`C:\ChromeMCPProfile`) dedicado a pruebas:

1. Crear un acceso directo en el Escritorio de Windows.
2. Definir como destino:
   ```text
   "C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --remote-allow-origins="*" --user-data-dir="C:\ChromeMCPProfile"
   ```
3. Nombrarlo: **Chrome DevTools MCP**.
4. Ejecutar el acceso directo para abrir la sesión de Chrome con el servidor CDP activo en el puerto `9222`.

---

### Paso 4: Validar Conectividad desde WSL2 (Fedora)

Con la ventana de Chrome abierta en Windows, verificar la respuesta JSON desde la terminal de Fedora:

```bash
curl -s http://localhost:9222/json/version
```

**Respuesta esperada (HTTP 200 OK):**
```json
{
   "Browser": "Chrome/156.x.x.x",
   "Protocol-Version": "1.3",
   "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
   "webSocketDebuggerUrl": "ws://localhost:9222/devtools/browser/..."
}
```

---

### Paso 5: Configuración de Servidor MCP en el Proyecto

Configurar el servidor MCP en el repositorio (`.mcp/config.json`), o basarse en `.mcp/config.example.json`:

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

---

### Paso 6: Recarga y Ejecución en Antigravity / Gestores de Agentes

1. En Antigravity (o VS Code con Claude Code / Codex / Roo Code):
   - Presionar `Ctrl + Shift + P` y ejecutar: **Developer: Reload Window**.
2. Al iniciar una tarea que requiera pruebas visuales, el agente detecta el servidor MCP, conecta con el endpoint de Windows y conduce la navegación directamente en tu pantalla.
