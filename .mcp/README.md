# Configuración de Servidores MCP (Model Context Protocol)

Este directorio contiene las configuraciones para integrar servidores MCP con agentes autónomos (**Google Antigravity**, **Claude Code**, **Codex**, **Cursor**, **Roo Code**, etc.).

---

## Servidores Disponibles

### 1. `chrome-browser` (@modelcontextprotocol/server-puppeteer)
Permite al agente controlar un navegador de forma interactiva y visual para pruebas de interfaz, flujos e2e y verificación de estado.

#### Conexión a Chrome en Windows desde WSL2:
1. Copia la plantilla de ejemplo si no tienes `config.json`:
   ```bash
   cp .mcp/config.example.json .mcp/config.json
   ```
2. Revisa la guía paso a paso completa en:
   👉 [`docs/lessons_learned/mcp-chrome-devtools-wsl2.md`](../docs/lessons_learned/mcp-chrome-devtools-wsl2.md)
3. Inicia Google Chrome en Windows con el puerto de depuración habilitado:
   ```text
   chrome.exe --remote-debugging-port=9222 --remote-allow-origins="*" --user-data-dir="C:\ChromeMCPProfile"
   ```
4. Recarga la ventana de tu editor/IDE (`Developer: Reload Window`).
