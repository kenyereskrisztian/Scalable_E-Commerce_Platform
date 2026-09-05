#!/usr/bin/env bash
set -euo pipefail

# -------------------------------------------------------------------
# VPS-deploy script for the Scalable E-Commerce Platform
# Tested on: Ubuntu 22.04 / Oracle Linux 8
# Usage:     sudo bash scripts/vps-deploy.sh          (first install)
#            bash   scripts/vps-deploy.sh update       (pull + restart)
# -------------------------------------------------------------------

REPO_URL="https://github.com/kenyereskrisztian/Scalable_E-Commerce_Platform.git"
APP_DIR="/opt/ecommerce"
COMMAND="${1:-}"

# ---------- helpers ----------
info()  { printf '\033[1;34m[INFO]\033[0m  %s\n' "$*"; }
ok()    { printf '\033[1;32m[OK]\033[0m    %s\n' "$*"; }
warn()  { printf '\033[1;33m[WARN]\033[0m  %s\n' "$*"; }
die()   { printf '\033[1;31m[FATAL]\033[0m %s\n' "$*" >&2; exit 1; }

generate_secret() {
  openssl rand -base64 32 | tr -d '/+='
}

# ---------- update mode ----------
if [ "$COMMAND" = "update" ]; then
  [ -d "$APP_DIR/.git" ] || die "Nem található a $APP_DIR – futtatsd előbb a telepítőt"
  info "Git pull..."
  cd "$APP_DIR" && git pull
  info "Compose up..."
  cd "$APP_DIR" && docker compose pull && docker compose up -d
  ok "Kész – következő egészség-ellenőrzés:"
  sleep 5
  docker compose -f "$APP_DIR/docker-compose.yml" ps
  exit 0
fi

# ---------- first-time install ----------
[ "$(id -u)" -eq 0 ] || die "Futtatsd rootként: sudo bash scripts/vps-deploy.sh"

info "=== 1/6 – Docker telepítés (ha hiányzik) ==="
if ! command -v docker &>/dev/null; then
  info "Docker telepítés..."
  if command -v apt-get &>/dev/null; then
    apt-get update -qq
    apt-get install -y -qq ca-certificates curl gnupg ufw > /dev/null
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
      https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
      > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin > /dev/null
  else
    # Oracle Linux / RHEL
    dnf install -y docker docker-compose-plugin
  fi
  systemctl enable --now docker
  ok "Docker telepítve"
else
  ok "Docker már telepítve"
fi

info "=== 2/6 – Tűzfal (ufw) ==="
if command -v ufw &>/dev/null; then
  ufw --force enable
  ufw allow 22/tcp  comment 'SSH'    >/dev/null
  ufw allow 80/tcp  comment 'HTTP'   >/dev/null
  ufw allow 443/tcp comment 'HTTPS'  >/dev/null
  ufw allow 8080/tcp comment 'Gateway' >/dev/null
  ufw allow 5500/tcp comment 'Frontend' >/dev/null
  ufw --force enable >/dev/null
  ok "ufw: 22,80,443,8080,5500 nyitva"
else
  warn "ufw nincs telepítve – a biztonsági csoport szabályokat az Oracle Cloud Console-ban kell beállítani"
fi

info "=== 3/6 – Repo klónozás ==="
if [ -d "$APP_DIR/.git" ]; then
  ok "Repo már létezik: $APP_DIR"
else
  git clone "$REPO_URL" "$APP_DIR"
  ok "Repo klónozva: $APP_DIR"
fi
cd "$APP_DIR"

info "=== 4/6 – .env konfiguráció ==="
if [ ! -f .env ]; then
  MYSQL_PW=$(generate_secret)
  JWT=$(openssl rand -base64 48)

  # Automatikus PUBLIC_IP lekérés
  PUBLIC_IP=$(curl -s --max-time 5 https://api.ipify.org || curl -s --max-time 5 https://ifconfig.me || echo "ENTER_YOUR_PUBLIC_IP")
  info "Felismert publikus IP: $PUBLIC_IP"

  cat > .env <<EOF
# === Generálva a vps-deploy.sh által — ne kézzel szerkeszd ===
MYSQL_ROOT_PASSWORD=$MYSQL_PW
JWT_SECRET=$JWT
PUBLIC_IP=$PUBLIC_IP

# Ha a GitHub csomagok nem publikusak, add meg egy Personal Access Token-t:
# DOCKER_TOKEN=ghp_...
EOF

  ok ".env létrehozva – ellenőrizd, hogy a PUBLIC_IP helyes!"
  warn "Ha a GHCR csomagok privátak, vinned kell egy GITHUB_TOKEN-t: echo 'DOCKER_TOKEN=ghp_...' >> .env"
else
  ok ".env már létezik"
fi

info "=== 5/6 – GHCR login (ha token megadva) ==="
if grep -q '^DOCKER_TOKEN=' .env; then
  TOKEN=$(grep '^DOCKER_TOKEN=' .env | cut -d= -f2)
  echo "$TOKEN" | docker login ghcr.io -u kenyereskrisztian --password-stdin
  ok "GHCR bejelentkezve"
else
  info "Nincs DOCKER_TOKEN – a csomagok valószínűleg publikusak, skip"
fi

info "=== 6/6 – Stack indítása ==="
docker compose pull
docker compose up -d

info "Várakozás a healthcheck-ekre (akár 5 perc – lassú ARM VM)..."
ATTEMPTS=0
MAX_WAIT=300
until [ "$(docker inspect --format='{{.State.Health.Status}}' $(docker compose ps -q api-gateway) 2>/dev/null)" = "healthy" ]; do
  ATTEMPTS=$((ATTEMPTS + 1))
  [ "$ATTEMPTS" -ge "$MAX_WAIT" ] && die "api-gateway nem lett egészséges $MAX_WAIT mp alatt"
  sleep 1
done
ok "api-gateway healthy"

info ""
info "============================================================"
info "  DEPLOY KÉSZ"
info "============================================================"
PUB=$(grep '^PUBLIC_IP=' .env | cut -d= -f2)
info "  Frontend:    http://$PUB:5500"
info "  API Gateway: http://$PUB:8080"
info "  Eureka:      SSH-tunnel a 127.0.0.1:8761-re"
info "  Grafana:     SSH-tunnel a 127.0.0.1:3000-re"
info "  Kibana:      SSH-tunnel a 127.0.0.1:5601-re"
info "  MySQL:       SSH-tunnel a 127.0.0.1:3306-ra"
info "============================================================"
