#!/bin/sh
set -eu

ESCAPED_URL=$(printf '%s' "${API_BASE_URL:-}" | sed 's/\\/\\\\/g; s/"/\\"/g')

cat > /tmp/env.js <<EOF
window.__ENV__ = {
  API_BASE_URL: "${ESCAPED_URL}"
};
EOF
