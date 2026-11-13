#!/usr/bin/env bash
# ==============================================================================
# Restauración (Restore Drill) PostgreSQL desde Respaldo Cifrado GPG (.sql.gz.gpg)
# Cumple TASK-606 / ADR-0026
# ==============================================================================
set -euo pipefail

usage() {
  cat <<'EOF'
Uso:
  restore-postgres-s3.sh --file <ruta.sql.gz.gpg> [--target-db <nombre_bd>] [opciones]
  restore-postgres-s3.sh --s3-key <postgres/db/archivo.sql.gz.gpg> [--target-db <nombre_bd>] [opciones]

Opciones:
  --file <path>               Archivo cifrado local (.sql.gz.gpg)
  --s3-key <key>              Clave del objeto en S3/OCI (requiere S3_ENDPOINT y BACKUP_S3_BUCKET)
  --target-db <db>            Base de datos destino (default: restaurant_restore_drill)
  --docker-container <name>   Ejecutar psql vía 'docker exec -i <name>'
  --confirm-prod              Confirmación explícita si --target-db es restaurant_prod
  --verify-only               Solo descifra, valida checksum/gzip e inspecciona SQL sin ejecutar psql
  -h, --help                  Mostrar esta ayuda
EOF
}

INPUT_FILE=""
S3_KEY=""
TARGET_DB="${TARGET_DB:-restaurant_restore_drill}"
DOCKER_CONTAINER="${DB_CONTAINER:-}"
CONFIRM_PROD="false"
VERIFY_ONLY="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --file)
      INPUT_FILE="$2"
      shift 2
      ;;
    --s3-key)
      S3_KEY="$2"
      shift 2
      ;;
    --target-db)
      TARGET_DB="$2"
      shift 2
      ;;
    --docker-container)
      DOCKER_CONTAINER="$2"
      shift 2
      ;;
    --confirm-prod)
      CONFIRM_PROD="true"
      shift
      ;;
    --verify-only)
      VERIFY_ONLY="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: Opción desconocida: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$INPUT_FILE" && -z "$S3_KEY" ]]; then
  echo "ERROR: Debes indicar --file o --s3-key." >&2
  usage >&2
  exit 1
fi

if [[ "$TARGET_DB" == "restaurant_prod" && "$CONFIRM_PROD" != "true" && "$VERIFY_ONLY" != "true" ]]; then
  echo "ERROR: Por seguridad, restaurar sobre 'restaurant_prod' requiere --confirm-prod explícito." >&2
  exit 1
fi

if [[ -z "${BACKUP_ENCRYPTION_PASSPHRASE:-}" ]]; then
  echo "ERROR: BACKUP_ENCRYPTION_PASSPHRASE es obligatorio para descifrar." >&2
  exit 1
fi

WORK_DIR="$(mktemp -d)"
export GNUPGHOME="${WORK_DIR}/gnupg"
mkdir -p "$GNUPGHOME"
chmod 700 "$GNUPGHOME"
cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

if [[ -n "$S3_KEY" ]]; then
  S3_ENDPOINT="${S3_ENDPOINT:-}"
  S3_REGION="${S3_REGION:-us-ashburn-1}"
  S3_BUCKET="${BACKUP_S3_BUCKET:-${S3_BUCKET:-}}"
  export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-${S3_ACCESS_KEY:-}}"
  export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-${S3_SECRET_KEY:-}}"
  export AWS_DEFAULT_REGION="$S3_REGION"

  if [[ -z "$S3_ENDPOINT" || -z "$S3_BUCKET" ]]; then
    echo "ERROR: S3_ENDPOINT y BACKUP_S3_BUCKET son obligatorios con --s3-key." >&2
    exit 1
  fi
  INPUT_FILE="${WORK_DIR}/$(basename "$S3_KEY")"
  echo "[1/3] Descargando s3://${S3_BUCKET}/${S3_KEY}..."
  aws --endpoint-url "$S3_ENDPOINT" s3 cp "s3://${S3_BUCKET}/${S3_KEY}" "$INPUT_FILE" --only-show-errors
fi

if [[ ! -f "$INPUT_FILE" ]]; then
  echo "ERROR: No existe el archivo '$INPUT_FILE'." >&2
  exit 1
fi

if [[ -f "${INPUT_FILE}.sha256" ]]; then
  echo "[*] Verificando checksum SHA-256..."
  (cd "$(dirname "$INPUT_FILE")" && sha256sum -c "$(basename "${INPUT_FILE}.sha256")")
fi

DECRYPTED_GZ="${WORK_DIR}/restore.sql.gz"
echo "[2/3] Descifrando archivo GPG..."
printf '%s' "$BACKUP_ENCRYPTION_PASSPHRASE" | gpg \
  --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
  --decrypt -o "$DECRYPTED_GZ" "$INPUT_FILE"

gzip -t "$DECRYPTED_GZ"

if [[ "$VERIFY_ONLY" == "true" ]]; then
  LINE_COUNT="$(gzip -dc "$DECRYPTED_GZ" | wc -l)"
  echo "[3/3] Verificación exitosa (--verify-only). Archivo SQL íntegro (${LINE_COUNT} líneas)."
  exit 0
fi

export PGHOST="${PGHOST:-${DB_HOST:-127.0.0.1}}"
export PGPORT="${PGPORT:-${DB_PORT:-5432}}"
export PGUSER="${PGUSER:-${DB_USER:-postgres}}"
if [[ -n "${DB_PASSWORD:-}" && -z "${PGPASSWORD:-}" ]]; then
  export PGPASSWORD="$DB_PASSWORD"
fi

echo "[3/3] Restaurando en base de datos destino '${TARGET_DB}'..."
if [[ -n "$DOCKER_CONTAINER" ]]; then
  gzip -dc "$DECRYPTED_GZ" | docker exec -i -e PGPASSWORD="${PGPASSWORD:-}" "$DOCKER_CONTAINER" \
    psql -v ON_ERROR_STOP=1 -U "$PGUSER" -d "$TARGET_DB"
else
  gzip -dc "$DECRYPTED_GZ" | psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$TARGET_DB"
fi

echo "Restauración completada exitosamente sobre '${TARGET_DB}'."
