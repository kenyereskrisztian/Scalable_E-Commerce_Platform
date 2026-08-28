#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "Scalable E-Commerce Platform - DEV mod inditasa (hot reload)"
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
