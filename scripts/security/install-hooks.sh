#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
HOOKS_DIR="$(git -C "$REPO_ROOT" rev-parse --git-path hooks)"
TARGET="${HOOKS_DIR}/pre-commit"

mkdir -p "$HOOKS_DIR"
cp "${SCRIPT_DIR}/pre-commit.template" "$TARGET"
chmod +x "$TARGET"

echo "Hook pre-commit DevSecOps instalado en: ${TARGET}"
