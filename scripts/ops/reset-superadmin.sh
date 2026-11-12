#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Break-Glass Recovery: Restablecimiento de Emergencia para SUPERADMIN (ADR-0027)
# Uso recomendado en VPS OCI vía SSH:
#   PLATFORM_ADMIN_USERNAME=superadmin ./scripts/ops/reset-superadmin.sh
# ==============================================================================

ADMIN_USER="${1:-${PLATFORM_ADMIN_USERNAME:-superadmin}}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose.yml}"
BACKEND_SERVICE="${BACKEND_SERVICE:-backend}"

if [[ -z "${PLATFORM_ADMIN_PASSWORD:-}" ]]; then
  read -rsp "Nueva contraseña temporal para '${ADMIN_USER}' (mín. 12 caracteres): " PLATFORM_ADMIN_PASSWORD
  echo
fi

if [[ ${#PLATFORM_ADMIN_PASSWORD} -lt 12 ]]; then
  echo "ERROR: La contraseña debe tener al menos 12 caracteres." >&2
  exit 1
fi

export PLATFORM_ADMIN_USERNAME="$ADMIN_USER"
export PLATFORM_ADMIN_PASSWORD

echo "Ejecutando recuperación Break-Glass para '${ADMIN_USER}' (forzará must_change_password=true y revocará sesiones activas)..."

if command -v docker >/dev/null 2>&1 && [[ -f "$COMPOSE_FILE" ]]; then
  docker compose -f "$COMPOSE_FILE" exec -T \
    -e PLATFORM_ADMIN_USERNAME="$PLATFORM_ADMIN_USERNAME" \
    -e PLATFORM_ADMIN_PASSWORD="$PLATFORM_ADMIN_PASSWORD" \
    "$BACKEND_SERVICE" \
    java -jar /app/app.jar \
      --server.port=0 \
      --app.platform.admin.break-glass-reset=true \
      --app.platform.admin.exit-after-reset=true
else
  echo "ERROR: No se encontró Docker o el archivo '$COMPOSE_FILE'. Ejecuta este script desde la raíz del despliegue." >&2
  exit 1
fi

echo "Recuperación completada. Inicia sesión en el Backoffice y cambia la contraseña inmediatamente."
