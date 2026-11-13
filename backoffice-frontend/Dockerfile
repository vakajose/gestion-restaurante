# syntax=docker/dockerfile:1.7

# ==============================================================================
# Stage 1: Build React 19 + Vite + TypeScript Backoffice SPA
# ==============================================================================
FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ==============================================================================
# Stage 2: Nginx Non-Root + Inyección Runtime de env.js
# ==============================================================================
FROM nginxinc/nginx-unprivileged:1.31-alpine AS runtime

USER 0
RUN apk upgrade --no-cache
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/40-generate-env.sh /docker-entrypoint.d/40-generate-env.sh
COPY --from=builder /app/dist /usr/share/nginx/html

RUN chmod +x /docker-entrypoint.d/40-generate-env.sh \
    && rm -f /usr/share/nginx/html/env.js \
    && ln -s /tmp/env.js /usr/share/nginx/html/env.js \
    && chown -R 101:101 /usr/share/nginx/html

ENV API_BASE_URL=""
USER 101:101
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:8080/healthz" | grep -q "ok" || exit 1
