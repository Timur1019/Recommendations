#!/usr/bin/env bash
# Деплой по SSH + rsync. Настройте вход по ключу: ssh-copy-id root@$DEPLOY_HOST
# Использование с корня репозитория: ./deploy/deploy.sh
# Переменные: DEPLOY_HOST (по умолчанию ниже), DEPLOY_REMOTE_DIR

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${DEPLOY_HOST:-85.198.81.217}"
REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/recommendations}"

echo "==> Rsync → root@${HOST}:${REMOTE_DIR}/"
ssh "root@${HOST}" "mkdir -p '${REMOTE_DIR}'"
rsync -az --delete \
  --exclude '.git' \
  --exclude '.env' \
  --exclude 'node_modules' \
  --exclude 'build' \
  --exclude '.gradle' \
  --exclude '.idea' \
  --exclude 'logs' \
  "${ROOT}/" "root@${HOST}:${REMOTE_DIR}/"

echo "==> Docker Compose (build + up -d)"
ssh "root@${HOST}" bash -s <<REMOTE
set -euo pipefail
cd "${REMOTE_DIR}"
if [[ ! -f .env ]]; then
  echo ">>> Внимание: нет файла .env — подставятся слабые значения из compose."
  echo ">>> Один раз: cp env.docker.example .env && nano .env"
fi
docker compose build
docker compose up -d
REMOTE

echo "==> Готово. Проверка: http://${HOST}/"
