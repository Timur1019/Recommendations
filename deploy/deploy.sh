#!/usr/bin/env bash
# Деплой по SSH + rsync на сервер.
#
# На сервере в REMOTE_DIR нет каталога .git (rsync его не копирует) — команда git pull ТАМ не сработает.
# Обновлять код: на Mac в репозитории `git pull`, затем снова этот скрипт.
#
# Вход на сервер (вручную):  ssh root@85.198.81.217
# Ключ один раз:            ssh-copy-id root@85.198.81.217
#
# Запуск с машины разработчика (из корня репозитория):
#   ./deploy/deploy.sh
# Другой хост/каталог:
#   DEPLOY_HOST=... DEPLOY_REMOTE_DIR=/opt/recommendations ./deploy/deploy.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${DEPLOY_HOST:-85.198.81.217}"
REMOTE_DIR="${DEPLOY_REMOTE_DIR:-/opt/recommendations}"

echo "==> Сервер: ssh root@${HOST}"
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
echo ""
echo "Если на сервере ошибка «backend unhealthy» / web не стартует:"
echo "  ssh root@${HOST} \"cd ${REMOTE_DIR} && docker compose ps && docker compose logs backend --tail=120\""
echo "Частая причина: в .env другой DB_PASSWORD, а том Postgres уже создан со старым паролем."
echo "  Один раз (УДАЛИТ данные БД в Docker): ssh root@${HOST} \"cd ${REMOTE_DIR} && docker compose down -v && docker compose up -d --build\""
echo "Проверка API из контейнера backend: docker compose exec backend curl -sf http://127.0.0.1:8080/api/health/live"
