# VPS-deploy (Oracle Cloud Always Free)

A teljes e-commerce stack egyetlen ingyenes Oracle Cloud ARM VM-en fut, Docker Compose-zal.
Az image-k a GHCR-ből (`ghcr.io/kenyereskrisztian/*`) jönnek, amit a CI automatikusan pushol main-re.

## Miért ingyenes

- **Oracle Cloud Always Free** ARM VM (`VM.Standard.A1.Flex`): 2 OCPU / 12 GB RAM / 200 GB disk / 10 TB sávszél — örökre ingyen.
  - (Korábban 4 OCPU / 24 GB volt; 2026 augusztustól 2 OCPU / 12 GB.)
- A service-ek egy Docker networken belül kommunikálnak → **nincs** per-service díj, nincs privát-hálózati korlát.
- Kapacitásproblémák lehetnek (ARM régiónként elfogyhat) — próbálj több availability domain/region kombinációt a létrehozáskor.

## Heti architecture

```
Interneten (publikus):
  http://<PUBLIC_IP>:5500   → frontend-service
  http://<PUBLIC_IP>:8080   → api-gateway  (Spring Cloud Gateway)

127.0.0.1-en (CSAK SSH-tunnel):
  8761  discovery-service
  8081  user-service, 8082 product, 8083 cart,
  8084  order, 8085 payment, 8086 notification
  3306  mysql (seed-et az első indításkor veti be)
  9090  prometheus, 3000 grafana, 9200 elasticsearch, 5601 kibana

Docker internal network:
  db, discovery-service, api-gateway, user/product/cart/order/payment/notification-service,
  frontend-service, prometheus, grafana, elasticsearch, logstash, kibana
```

## Telepítés

### 1. Oracle Cloud felkészítés

1. Regisztráció: https://www.oracle.com/cloud/free/ (bankkártya kell az azonosításhoz, de **nem vonnak le** semmit)
2. Compute → Instances → Create instance
   - Image: **Ubuntu 22.04** (a script ehhez van tesztelve)
   - Shape: **VM.Standard.A1.Flex**, 2 OCPU / 12 GB RAM
   - Add SSH public key (a saját géped kulcsa)
   - VCN/Subnet alap automatikus
3. VCN → Security List → Ingress rules: **22, 80, 443, 8080, 5500** (a script a host-tűzfalat is beállítja, de az OCI security list is muszáj)

### 2. Repo a VM-re

Nem kell külön cloneozni, a script elvégzi. Csak SSH-val jelentkezz be:

```bash
ssh -i ~/.ssh/oracle_key ubuntu@<PUBLIC_IP>
```

### 3. Deploy futtatása

```bash
git clone https://github.com/kenyereskrisztian/Scalable_E-Commerce_Platform.git
cd Scalable_E-Commerce_Platform
sudo bash scripts/vps-deploy.sh
```

A script:
1. telepíti a Dockert (ha hiányzik),
2. beállítja az ufw tűzfalat (22, 80, 443, 8080, 5500),
3. klónozza a repót `/opt/ecommerce`-be,
4. generál egy `.env`-t (`MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `PUBLIC_IP`),
5. bejelentkezik a GHCR-be ha `DOCKER_TOKEN` van megadva,
6. pull + up -d, vár az api-gateway health-ére.

### Fontos: GHCR csomagok

Ha a GitHub repo (és így a GHCR csomagok) **privát**, Docker-login kell:

```bash
echo 'DOCKER_TOKEN=ghp_...' >> /opt/ecommerce/.env
cd /opt/ecommerce && docker compose pull
```

(Token: GitHub → Settings → Developer settings → Personal access tokens, `read:packages` jog.)

## Frissítés

```bash
cd /opt/ecommerce
bash scripts/vps-deploy.sh update
```

(Git pull + image pull + restart.)

## Ellenőrzés

```bash
docker compose -f /opt/ecommerce/docker-compose.yml ps
curl http://localhost:8080/actuator/health    # gateway
curl http://localhost:8761/actuator/health    # eureka
```

A 127.0.0.1-re bindolt UI-kat SSH-tunnellel éred el:

```bash
ssh -i ~/.ssh/oracle_key -L 8761:localhost:8761 -L 3000:localhost:3000 -L 5601:localhost:5601 ubuntu@<PUBLIC_IP>
# ezek után helyben:  http://localhost:8761, :3000, :5601
```

## ELK / monitoring — ami NEM indul automatikusan

**Fontos: az ELK-stack (elasticsearch, logstash, kibana) a `profiles: ["elk"]` mögött van, ezért a `docker compose up -d` NEM indítja el őket.** Ha nem kell a loggyűjtés, ez így is marad — a `prometheus` és a `grafana` viszont mindig fut.

Ezeket manuálisan kell elindítani, ha kellenek:

```bash
cd /opt/ecommerce && sudo docker compose --profile elk up -d
```

Ha az ELK nincs fent, a service-ek logstash-appendere csendben, async újrapróbálkozik — nem blokkol, de a `docker logs`-ban WARN üzenetek jelenhetnek meg. Az ELK leállításához:

```bash
cd /opt/ecommerce && sudo docker compose --profile elk stop
```

Indítás/kikapcsolás után a ["Ellenőrzés"](#ellenőrzés) szakaszbeli `ps`-sel ellenőrizd, hogy mit futtatsz.

## HTTPS / saját domain (certbot + DuckDNS + nginx)

A platform a **`https://kkrisztian-ecommerce.duckdns.org`** címen érhető el. A Docker-stack ettől függetlenül fut; az nginx reverse proxy a kéréseket a frontendre és a gateway-re továbbítja.

### Architektúra

```
https://kkrisztian-ecommerce.duckdns.org
        │  (DuckDNS A-rekord → PUBLIC_IP)
        ▼
     nginx :443 (Let's Encrypt tanúsítvány)
     ├── /      → 127.0.0.1:5500   (frontend-service)
     └── /api/  → 127.0.0.1:8080   (api-gateway)
```

- A frontend `API_BASE_URL`-ja a compose-ban a domainre mutat (`https://kkrisztian-ecommerce.duckdns.org`), így **same-origin** a hívás — nincs CORS- és mixed content-probléma.
- A `:80` HTTP-kérés 301-gyel átirányul `https://`-re.

### Nginx config (`/etc/nginx/sites-available/…`)

```nginx
server {
    listen 80;
    server_name kkrisztian-ecommerce.duckdns.org;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name kkrisztian-ecommerce.duckdns.org;

    ssl_certificate     /etc/letsencrypt/live/kkrisztian-ecommerce.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/kkrisztian-ecommerce.duckdns.org/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://127.0.0.1:5500;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;   # záró / NÉLKÜL, hogy az /api/ út megmaradjon
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Config betöltése:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

> **Figyelem:** a `/api/` blokkban a `proxy_pass` végén **ne** legyen záró `/` (`proxy_pass http://127.0.0.1:8080;`). Záró `/`-tel az nginx levágja az `/api/` prefixet, és a gateway `404`-et ad.

### Certbot — tanúsítvány kiadása és MEGÚJÍTÁSA

A tanúsítványt **manuális DNS-challenge**-szel adjuk ki (DuckDNS-nél ez nem automatikus):

```bash
sudo certbot certonly --manual --preferred-challenges dns -d kkrisztian-ecommerce.duckdns.org
```

**⚠️ Fontos: a tanúsítvány 90 napig él, és a `--manual` mód miatt NINCS automatikus megújítás.** A példány újraindítása nem számít bele — a certbot kéri le újra, ha lejárt.

Megújítás (90 naponként, kézzel):

```bash
sudo certbot certonly --manual --preferred-challenges dns -d kkrisztian-ecommerce.duckdns.org --force-renewal
sudo nginx -t && sudo systemctl reload nginx
```

A megújítás során a certbot egy `_acme-challenge.kkrisztian-ecommerce` TXT-rekord felvételét kéri a DuckDNS felületén; a parancs megmondja a pontos értéket.

## Megjegyzések

- A `.env` tartalmazza a `MYSQL_ROOT_PASSWORD`-t és a `JWT_SECRET`-et — fontos: minden futó JWT a `JWT_SECRET`-tel fut, ha megváltoztatod, minden token érvénytelenné válik.
- A MySQL seed csak **egyszer** fut (üres volumen esetén). Adatvesztés nélküli újrascemeléshez: `docker compose down -v` (⚠️ törli az adatot).