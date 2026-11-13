#!/usr/bin/env bash
# ==============================================================================
# Runner Unificado DevSecOps Local (TASK-607 / PB-058)
# Ejecuta:
#   1. Detección de Secretos (gitleaks + verificación de archivos rastreados)
#   2. SAST Backend Java 25 (Maven SpotBugs + FindSecBugs)
#   3. SCA Node/npm (npm audit --omit=dev --audit-level=high en ambos frontends)
#   4. Auditoría básica de Dockerfiles (Non-Root / sin secretos en ENV)
#   5. Escaneo opcional de FS/Imágenes con Trivy (si está instalado o vía Docker)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

MODE="${1:---full}"
FAILURES=0

banner() {
  echo ""
  echo "======================================================================"
  echo " $1"
  echo "======================================================================"
}

pass() { echo "[PASS] $1"; }
warn() { echo "[WARN] $1"; }
fail() { echo "[FAIL] $1" >&2; FAILURES=$((FAILURES + 1)); }

cd "$ROOT_DIR"

# ------------------------------------------------------------------------------
# 1. Secretos en Git / Árbol de Trabajo
# ------------------------------------------------------------------------------
banner "1/5 Auditoría de Secretos (Git & Working Tree)"
TRACKED_SECRETS="$(git ls-files 2>/dev/null | grep -E '(^|/)\.env(\.[^.]+)?$|\.(pem|key|p12|pfx)$' | grep -Ev '\.example$|\.template$' || true)"
if [[ -n "$TRACKED_SECRETS" ]]; then
  fail "Archivos sensibles versionados en Git:\n${TRACKED_SECRETS}"
else
  pass "0 archivos .env/llaves privadas rastreados por Git."
fi

if command -v gitleaks >/dev/null 2>&1; then
  if gitleaks detect --source "$ROOT_DIR" --config "${ROOT_DIR}/.gitleaks.toml" --redact --no-banner; then
    pass "Gitleaks: 0 secretos detectados en historial y árbol de trabajo."
  else
    fail "Gitleaks detectó posibles secretos."
  fi
elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  GITLEAKS_SUBCMD="git ."
  if [[ -f "${ROOT_DIR}/.git" ]]; then
    GITLEAKS_SUBCMD="dir ."
  fi
  if docker run --rm -w /repo -v "${ROOT_DIR}:/repo:ro" zricethezav/gitleaks:latest ${GITLEAKS_SUBCMD} --config=/repo/.gitleaks.toml --redact --no-banner; then
    pass "Gitleaks (Docker): 0 secretos detectados en el repositorio."
  else
    fail "Gitleaks (Docker) detectó posibles secretos."
  fi
else
  warn "Binario 'gitleaks' no instalado localmente; ejecutando fallback regex en archivos fuente."
  PATTERN_HITS="$(grep -rnE --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target --exclude-dir=dist --exclude-dir=.angular \
    '-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|AIza[0-9A-Za-z_-]{35}' \
    backend/src frontend/src backoffice-frontend/src docker/ scripts/ 2>/dev/null || true)"
  if [[ -n "$PATTERN_HITS" ]]; then
    fail "Patrones de llaves/tokens detectados:\n${PATTERN_HITS}"
  else
    pass "Fallback regex: 0 llaves privadas o tokens de nube hardcodeados en código fuente."
  fi
fi

# ------------------------------------------------------------------------------
# 2. Auditoría de Dockerfiles (Non-Root + Hardening)
# ------------------------------------------------------------------------------
banner "2/5 Hardening de Dockerfiles (Non-Root / Twelve-Factor)"
for df in backend/Dockerfile frontend/Dockerfile backoffice-frontend/Dockerfile; do
  if [[ ! -f "$df" ]]; then
    fail "Falta Dockerfile esperado: $df"
    continue
  fi
  if grep -Eq '^\s*USER\s+(1001|101|appuser|nginx)' "$df"; then
    pass "${df}: ejecuta con usuario non-root."
  else
    fail "${df}: no declara USER non-root en etapa runtime."
  fi
done

if [[ "$MODE" == "--quick" ]]; then
  banner "Modo --quick completado (failures=${FAILURES})"
  exit "$FAILURES"
fi

# ------------------------------------------------------------------------------
# 3. Backend Java 25 SAST (SpotBugs + FindSecBugs)
# ------------------------------------------------------------------------------
banner "3/5 SAST Backend Java 25 (SpotBugs + FindSecBugs)"
if (cd "${ROOT_DIR}/backend" && ./mvnw -B compile spotbugs:check -Psecurity); then
  pass "SpotBugs + FindSecBugs (threshold=High): 0 hallazgos bloqueantes."
else
  fail "SpotBugs + FindSecBugs reportó vulnerabilidades o error de compilación."
fi

# ------------------------------------------------------------------------------
# 4. SCA Frontends (npm audit producción sin HIGH/CRITICAL)
# ------------------------------------------------------------------------------
banner "4/5 SCA Frontends (npm audit --omit=dev --audit-level=high)"
for fe in frontend backoffice-frontend; do
  if (cd "${ROOT_DIR}/${fe}" && npm audit --omit=dev --audit-level=high); then
    pass "${fe}: 0 vulnerabilidades HIGH/CRITICAL en dependencias de producción."
  else
    fail "${fe}: npm audit reportó vulnerabilidades HIGH/CRITICAL en producción."
  fi
done

# ------------------------------------------------------------------------------
# 5. Escaneo Integral Trivy (FS / Config / Imágenes Docker)
# ------------------------------------------------------------------------------
banner "5/5 Escaneo Integral Trivy (FS / Config / Imágenes)"
run_trivy() {
  if command -v trivy >/dev/null 2>&1; then
    trivy "$@"
  else
    M2_MOUNT=""
    if [[ -d "${HOME}/.m2" ]]; then
      M2_MOUNT="-v ${HOME}/.m2:/root/.m2:ro"
    fi
    # shellcheck disable=SC2086
    docker run --rm \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -v "${ROOT_DIR}:/workspace:ro" \
      ${M2_MOUNT} \
      -v trivy_cache:/root/.cache/ \
      aquasec/trivy:latest "$@"
  fi
}

if command -v trivy >/dev/null 2>&1 || (command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1); then
  TRIVY_TARGET="$ROOT_DIR"
  if ! command -v trivy >/dev/null 2>&1; then
    TRIVY_TARGET="/workspace"
  fi

  if run_trivy fs --offline-scan --scanners vuln,misconfig --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 \
      --skip-dirs "**/node_modules,**/target,**/dist,**/.angular" "$TRIVY_TARGET"; then
    pass "Trivy FS + Misconfig scan limpio (0 HIGH/CRITICAL con fix)."
  else
    fail "Trivy FS + Misconfig detectó hallazgos HIGH/CRITICAL."
  fi

  if [[ "$MODE" == "--images" ]] && command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    for img in $(docker images --format '{{.Repository}}:{{.Tag}}' | grep -E '^(docker|restaurant-testing-local|ghcr\.io/.*/gestion-restaurante)-(backend|erp-frontend|backoffice-frontend|erp|backoffice):' || true); do
      if run_trivy image --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 "$img"; then
        pass "Trivy Image (${img}): 0 vulnerabilidades HIGH/CRITICAL con fix."
      else
        fail "Trivy Image (${img}): detectó vulnerabilidades HIGH/CRITICAL."
      fi
    done
  fi
else
  warn "Ni 'trivy' ni Docker activo detectados; omitiendo escaneo Trivy local (se ejecuta en CI/CD)."
fi

banner "RESUMEN DEVSECOPS (failures=${FAILURES})"
exit "$FAILURES"
