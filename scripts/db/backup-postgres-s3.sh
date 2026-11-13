#!/usr/bin/env bash
# ==============================================================================
# Respaldo Automatizado Encriptado PostgreSQL -> OCI Object Storage (S3 API)
# Cumple TASK-606 / ADR-0026: pg_dump comprimido + cifrado AES-256 (GPG) + retención
# ==============================================================================
set -euo pipefail

usage() {
  cat <<'EOF'
Uso:
  backup-postgres-s3.sh [opciones]

Variables de entorno requeridas:
  PGHOST / DB_HOST              Host PostgreSQL (default: 127.0.0.1)
  PGPORT / DB_PORT              Puerto PostgreSQL (default: 5432)
  PGDATABASE / DB_NAME          Base de datos (default: restaurant_prod)
  PGUSER / DB_USER              Usuario PostgreSQL (default: app_prod)
  PGPASSWORD / DB_PASSWORD      Contraseña PostgreSQL (opcional si usa .pgpass)
  BACKUP_ENCRYPTION_PASSPHRASE  Passphrase AES-256 para cifrado simétrico GPG

Variables S3/OCI (omitir solo con --local-only):
  S3_ENDPOINT                   Endpoint S3 OCI (ej. https://<ns>.compat.objectstorage.us-ashburn-1.oraclecloud.com)
  S3_REGION                     Región OCI (default: us-ashburn-1)
  BACKUP_S3_BUCKET              Bucket destino (ej. restaurant-db-backups)
  AWS_ACCESS_KEY_ID / S3_ACCESS_KEY
  AWS_SECRET_ACCESS_KEY / S3_SECRET_KEY
  BACKUP_RETENTION_DAYS         Días de retención local/remota (default: 14)

Opciones:
  --local-only                  Genera el respaldo cifrado en --output-dir sin subir a S3
  --output-dir <dir>            Directorio local temporal/archivo (default: /tmp/restaurant-backups)
  --docker-container <name>     Ejecutar pg_dump vía 'docker exec <name>' en lugar de pg_dump local
  -h, --help                    Mostrar esta ayuda
EOF
}

LOCAL_ONLY="false"
OUTPUT_DIR="${BACKUP_OUTPUT_DIR:-/tmp/restaurant-backups}"
DOCKER_CONTAINER="${DB_CONTAINER:-}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --local-only)
      LOCAL_ONLY="true"
      shift
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    --docker-container)
      DOCKER_CONTAINER="$2"
      shift 2
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

export PGHOST="${PGHOST:-${DB_HOST:-127.0.0.1}}"
export PGPORT="${PGPORT:-${DB_PORT:-5432}}"
export PGDATABASE="${PGDATABASE:-${DB_NAME:-restaurant_prod}}"
export PGUSER="${PGUSER:-${DB_USER:-app_prod}}"
if [[ -n "${DB_PASSWORD:-}" && -z "${PGPASSWORD:-}" ]]; then
  export PGPASSWORD="$DB_PASSWORD"
fi

if [[ -z "${BACKUP_ENCRYPTION_PASSPHRASE:-}" ]]; then
  echo "ERROR: BACKUP_ENCRYPTION_PASSPHRASE es obligatorio para cifrar el respaldo." >&2
  exit 1
fi

if ! command -v gpg >/dev/null 2>&1; then
  echo "ERROR: 'gpg' no está instalado." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
chmod 700 "$OUTPUT_DIR"

GNUPG_TMP="$(mktemp -d)"
export GNUPGHOME="$GNUPG_TMP"
chmod 700 "$GNUPG_TMP"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BASE_NAME="${PGDATABASE}_${TIMESTAMP}.sql.gz"
RAW_GZ_FILE="${OUTPUT_DIR}/${BASE_NAME}"
ENC_FILE="${RAW_GZ_FILE}.gpg"
SHA_FILE="${ENC_FILE}.sha256"

cleanup_plain() {
  rm -f "$RAW_GZ_FILE"
  rm -rf "$GNUPG_TMP"
}
trap cleanup_plain EXIT

echo "[1/4] Ejecutando pg_dump comprimido de '${PGDATABASE}' (${PGHOST}:${PGPORT})..."
if [[ -n "$DOCKER_CONTAINER" ]]; then
  docker exec -e PGPASSWORD="${PGPASSWORD:-}" "$DOCKER_CONTAINER" \
    pg_dump -U "$PGUSER" -d "$PGDATABASE" --no-owner --no-privileges --clean --if-exists \
    | gzip -9 > "$RAW_GZ_FILE"
else
  if ! command -v pg_dump >/dev/null 2>&1; then
    echo "ERROR: 'pg_dump' no encontrado y no se especificó --docker-container." >&2
    exit 1
  fi
  pg_dump -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    --no-owner --no-privileges --clean --if-exists \
    | gzip -9 > "$RAW_GZ_FILE"
fi

echo "[2/4] Cifrando respaldo con AES-256 (GPG simétrico)..."
printf '%s' "$BACKUP_ENCRYPTION_PASSPHRASE" | gpg \
  --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
  --symmetric --cipher-algo AES256 \
  -o "$ENC_FILE" "$RAW_GZ_FILE"

rm -f "$RAW_GZ_FILE"
sha256sum "$ENC_FILE" > "$SHA_FILE"
chmod 600 "$ENC_FILE" "$SHA_FILE"

echo "[3/4] Rotando respaldos locales antiguos (> ${RETENTION_DAYS} días)..."
find "$OUTPUT_DIR" -maxdepth 1 -type f \( -name "${PGDATABASE}_*.sql.gz.gpg" -o -name "${PGDATABASE}_*.sha256" \) -mtime +"$RETENTION_DAYS" -delete

if [[ "$LOCAL_ONLY" == "true" ]]; then
  echo "[4/4] Modo --local-only activo. Artefacto listo: ${ENC_FILE}"
  exit 0
fi

S3_ENDPOINT="${S3_ENDPOINT:-}"
S3_REGION="${S3_REGION:-us-ashburn-1}"
S3_BUCKET="${BACKUP_S3_BUCKET:-${S3_BUCKET:-}}"
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-${S3_ACCESS_KEY:-}}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-${S3_SECRET_KEY:-}}"
export AWS_DEFAULT_REGION="$S3_REGION"

if [[ -z "$S3_ENDPOINT" || -z "$S3_BUCKET" || -z "$AWS_ACCESS_KEY_ID" || -z "$AWS_SECRET_ACCESS_KEY" ]]; then
  echo "ERROR: Faltan variables S3 (S3_ENDPOINT, BACKUP_S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY)." >&2
  exit 1
fi

REMOTE_PREFIX="postgres/${PGDATABASE}"
echo "[4/4] Subiendo respaldo cifrado a s3://${S3_BUCKET}/${REMOTE_PREFIX}/..."
if command -v aws >/dev/null 2>&1; then
  aws --endpoint-url "$S3_ENDPOINT" s3 cp "$ENC_FILE" "s3://${S3_BUCKET}/${REMOTE_PREFIX}/$(basename "$ENC_FILE")" --only-show-errors
  aws --endpoint-url "$S3_ENDPOINT" s3 cp "$SHA_FILE" "s3://${S3_BUCKET}/${REMOTE_PREFIX}/$(basename "$SHA_FILE")" --only-show-errors
else
  echo "ERROR: 'aws' CLI no instalado. Instala AWS CLI v2 o usa --local-only + rclone." >&2
  exit 1
fi

echo "Respaldo completado y verificado exitosamente: $(basename "$ENC_FILE")"
