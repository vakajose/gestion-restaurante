#!/usr/bin/env bash
# =============================================================================
# SCRIPT DE ADMINISTRACIÓN Y RESETEO DE BASE DE DATOS
# Proyecto: Restaurant POS ERP
# =============================================================================

set -e

CONTAINER_NAME="${DB_CONTAINER:-restaurant-postgres-dev}"
DB_NAME="${1:-restaurant_dev}"
DB_USER="${DB_USER:-postgres}"
MODE="${2:---purge}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/reset_database.sql"

usage() {
    echo "Uso: $0 [NOMBRE_BD] [--purge | --full-recreate]"
    echo ""
    echo "Opciones de Base de Datos:"
    echo "  restaurant_dev  (Base de datos de desarrollo actual, default)"
    echo "  restaurant_db   (Base de datos previa / legacy)"
    echo ""
    echo "Modos:"
    echo "  --purge          Vacía tablas operativas/transaccionales (ventas, turnos, kardex) preservando maestros."
    echo "  --full-recreate  Elimina y recrea la base de datos por completo para forzar ejecución de Flyway V1-V10."
    echo ""
    echo "Ejemplos:"
    echo "  $0 restaurant_dev --purge"
    echo "  $0 restaurant_dev --full-recreate"
    exit 1
}

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    usage
fi

# Validar que el contenedor Docker esté corriendo
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}\$"; then
    echo "❌ Error: El contenedor Docker '${CONTAINER_NAME}' no está en ejecución."
    exit 1
fi

echo "======================================================================"
echo "🐘 GESTIÓN DE BASE DE DATOS POSTGRESQL (${CONTAINER_NAME})"
echo "   Base de datos: ${DB_NAME}"
echo "   Modo:         ${MODE}"
echo "======================================================================"

case "$MODE" in
    --purge)
        echo "🧹 Purgando datos transaccionales en '${DB_NAME}'..."
        docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}" < "${SQL_FILE}"
        echo "✅ Purga transaccional completada con éxito en '${DB_NAME}'."
        ;;
    --full-recreate)
        echo "⚠️  ADVERTENCIA: Recreando base de datos completa '${DB_NAME}'..."
        docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();"
        docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -c "DROP DATABASE IF EXISTS ${DB_NAME};"
        docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -c "CREATE DATABASE ${DB_NAME};"
        echo "✅ Base de datos '${DB_NAME}' recreada en limpio."
        echo "ℹ️  Inicia el backend ('./mvnw spring-boot:run') para aplicar Flyway V1-V10 y cargar Seeds."
        ;;
    *)
        echo "❌ Modo no reconocido: ${MODE}"
        usage
        ;;
esac
