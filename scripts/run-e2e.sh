#!/usr/bin/env bash
# ==============================================================================
# Script de Ejecución de Pruebas E2E (Playwright)
# Sistema de Gestión de Restaurante Multi-Tenant
# ==============================================================================
set -e

# Colores para salida de terminal
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}  Suite de Pruebas E2E - Regresión Operativa Completa  ${NC}"
echo -e "${BLUE}======================================================${NC}"

# 1. Verificar Node.js en PATH
if command -v node >/dev/null 2>&1; then
    NODE_VERSION=$(node -v)
    echo -e "${GREEN}✓ Node.js detectado: ${NODE_VERSION}${NC}"
else
    if [ -d "$HOME/.nvm/versions/node" ]; then
        LATEST_NODE=$(ls -td "$HOME/.nvm/versions/node"/v* 2>/dev/null | head -n 1)
        if [ -n "$LATEST_NODE" ]; then
            export PATH="$LATEST_NODE/bin:$PATH"
            echo -e "${GREEN}✓ NVM Node.js cargado: $(node -v)${NC}"
        fi
    fi
fi

# 2. Verificar disponibilidad de los servidores
echo -e "\n${BLUE}[1/2] Verificando estado de los servicios...${NC}"

BACKEND_UP=false
FRONTEND_UP=false

if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/auth/login >/dev/null 2>&1; then
    BACKEND_UP=true
    echo -e "${GREEN}✓ Backend Spring Boot activo en http://localhost:8080${NC}"
else
    echo -e "${YELLOW}⚠ Backend no responde en http://localhost:8080${NC}"
    echo -e "  Asegúrate de levantarlo con: ./backend/mvnw spring-boot:run"
fi

if curl -s -o /dev/null -w "%{http_code}" http://localhost:4200 >/dev/null 2>&1; then
    FRONTEND_UP=true
    echo -e "${GREEN}✓ Frontend Angular activo en http://localhost:4200${NC}"
else
    echo -e "${YELLOW}⚠ Frontend no responde en http://localhost:4200${NC}"
    echo -e "  Asegúrate de levantarlo con: cd frontend && npm start"
fi

if [ "$BACKEND_UP" = false ] || [ "$FRONTEND_UP" = false ]; then
    echo -e "\n${RED}Error: Ambos servidores deben estar en ejecución para correr las pruebas E2E.${NC}"
    echo -e "Por favor inicia los servicios y vuelve a ejecutar este script."
    exit 1
fi

# 3. Localizar binario de Playwright
PLAYWRIGHT_BIN=""
if [ -x "$FRONTEND_DIR/node_modules/.bin/playwright" ]; then
    PLAYWRIGHT_BIN="$FRONTEND_DIR/node_modules/.bin/playwright"
elif [ -x "/home/jvaca/proyectos/gestion-restaurante/frontend/node_modules/.bin/playwright" ]; then
    PLAYWRIGHT_BIN="/home/jvaca/proyectos/gestion-restaurante/frontend/node_modules/.bin/playwright"
else
    PLAYWRIGHT_BIN="npx playwright"
fi

echo -e "\n${BLUE}[2/2] Ejecutando especificaciones E2E con Playwright...${NC}"
cd "$FRONTEND_DIR"

# Pasar argumentos adicionales si el usuario los especificó (ej: --headed, --ui, shift-lifecycle.spec.ts)
"$PLAYWRIGHT_BIN" test --config=playwright.config.ts "$@"

TEST_EXIT_CODE=$?

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "\n${GREEN}======================================================${NC}"
    echo -e "${GREEN}  ✓ TODAS LAS PRUEBAS E2E PASARON EXITOSAMENTE        ${NC}"
    echo -e "${GREEN}======================================================${NC}"
else
    echo -e "\n${RED}======================================================${NC}"
    echo -e "${RED}  ✘ ALGUNAS PRUEBAS E2E FALLARON (Código: $TEST_EXIT_CODE)    ${NC}"
    echo -e "${RED}  Ver reporte con: npx playwright show-report         ${NC}"
    echo -e "${RED}======================================================${NC}"
fi

exit $TEST_EXIT_CODE
