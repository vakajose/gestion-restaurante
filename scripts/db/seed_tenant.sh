#!/usr/bin/env bash
# =============================================================================
# SCRIPT DE EJECUCIÓN DE SEMILLA BAJO DEMANDA PARA TENANCY NUEVO
# Proyecto: Restaurant POS ERP
# =============================================================================

set -e

CONTAINER_NAME="${DB_CONTAINER:-restaurant-postgres-dev}"
DB_NAME="${1:-restaurant_dev}"
DB_USER="${DB_USER:-postgres}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/seed_tenant.sql"

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "Uso: $0 [NOMBRE_BD]"
    echo ""
    echo "Ejecuta el script SQL 'scripts/db/seed_tenant.sql' para crear un tenancy completo"
    echo "con sucursales, roles, usuarios, catálogo, recetas, stock y turnos abiertos."
    echo ""
    echo "Ejemplo:"
    echo "  $0 restaurant_dev"
    exit 0
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}\$"; then
    echo "❌ Error: El contenedor Docker '${CONTAINER_NAME}' no está en ejecución."
    exit 1
fi

echo "======================================================================"
echo "🌱 EJECUTANDO SEMILLA DE TENANCY EN '${DB_NAME}' (${CONTAINER_NAME})"
echo "======================================================================"

docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" < "${SQL_FILE}"

echo "======================================================================"
echo "✅ Semilla de tenancy aplicada exitosamente en '${DB_NAME}'."
echo "======================================================================"
