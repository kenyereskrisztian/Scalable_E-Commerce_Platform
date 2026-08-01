#!/usr/bin/env bash
# A frontend tesztfelület indítása.
# Használat: scripts/serve-frontend.sh [port]  (alapértelmezett: 5500)
set -euo pipefail
cd "$(dirname "$0")/../frontend"
PORT="${1:-5500}"
echo "Tesztfelület: http://localhost:${PORT}"
exec python3 -m http.server "${PORT}"
