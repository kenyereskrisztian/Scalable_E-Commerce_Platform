# Scalable E-Commerce Platform

**Skálázható e-commerce platform mikroszolgáltatás-architektúrával**

[![CI](https://github.com/kenyereskrisztian/Scalable_E-Commerce_Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/kenyereskrisztian/Scalable_E-Commerce_Platform/actions/workflows/ci.yml)

---

## Áttekintés

Egy teljes körű, **10 mikroszolgáltatásból** álló e-commerce platform, amely Spring Boot és Docker Compose segítségével fut egy **Oracle Cloud Always Free ARM VM-en**. A rendszer a teljes vásárlási folyamatot lefedi: regisztráció, termékkatalógus, kosár, rendelés, fizetés és értesítés — mindez egy egységes API Gateway-en keresztül.

## Architektúra

```
                          ┌─────────────────────────────────┐
                          │        Internet (publikus)       │
                          │   :5500 (frontend)  :8080 (API)  │
                          └──────────┬──────────┬───────────┘
                                     │          │
                    ┌────────────────┴──┐  ┌────┴──────────────┐
                    │ frontend-service  │  │    api-gateway     │
                    │   (Spring Boot)   │  │ (Spring Cloud GW)  │
                    └───────────────────┘  └────┬──────────────┘
                                                │
         ┌──────────┬──────────┬────────────────┼────────────────┐
         │          │          │                │                │
    ┌────┴───┐ ┌────┴───┐ ┌───┴────┐ ┌────┴────┐ ┌────┴────────┐
    │  user  │ │product │ │  cart  │ │  order  │ │  payment    │
    │service │ │service │ │service │ │ service │ │  service    │
    └────┬───┘ └────┬───┘ └───┬────┘ └────┬────┘ └─────────────┘
         │          │         │            │
         │      ┌───┴────┐ ┌──┴─────┐     │
         │      │product │ │  cart  │     │
         │      │  DB    │ │   DB   │     │
    ┌────┴──┐   └────────┘ └────────┘     │
    │ user  │                              │
    │  DB   │    ┌──────────┐        ┌─────┴──────────┐
    └───────┘    │   db     │        │ notification   │
                 │ (MySQL)  │        │    service     │
                 └──────────┘        └────────────────┘

         ┌───────────────────────────────────────────┐
         │              discovery-service             │
         │            (Netflix Eureka)                │
         └───────────────────────────────────────────┘

         ┌──────────────┐  ┌─────────┐  ┌──────────┐
         │  prometheus  │→ │ grafana │  │  ELK     │
         │  (metrikák)  │  │(dashboard)│ │(naplózás)│
         └──────────────┘  └─────────┘  └──────────┘
```

## Mikroszolgáltatások

| Szolgáltás | Port | Adatbázis | Feladat |
|---|---|---|---|
| **api-gateway** | 8080 | — | Központi belépési pont, request routing, JWT autentikáció |
| **discovery-service** | 8761 | — | Netflix Eureka service discovery |
| **user-service** | 8081 | `ecommerce_users` | Regisztráció, bejelentkezés, felhasználók kezelése |
| **product-service** | 8082 | `ecommerce_products` | Termékek és kategóriák CRUD, készletkezelés |
| **cart-service** | 8083 | `ecommerce_cart` | Kosár kezelés (hozzáadás, mennyiség, törlés) |
| **order-service** | 8084 | `ecommerce_orders` | Rendelések leadása és nyomon követése |
| **payment-service** | 8085 | — | Fizetések feldolgozása (mock) |
| **notification-service** | 8086 | — | Értesítések küldése (email/SMS alapú) |
| **frontend-service** | 5500 | — | Tesztfelület (Vanilla JS, SPA-szerű UI) |
| **db** | 3306 | MySQL 8.4 | Központi adatbázis (4 séma) |

## Technológiai stack

**Backend:**
- Java 17, Spring Boot 3.2.5
- Spring Cloud Netflix (Eureka, Gateway)
- Spring Data JPA + MySQL Connector
- JWT autentikáció (jjwt 0.12.5)
- Lombok, MapStruct
- Spring Boot Actuator (health, metrikák)

**Frontend:**
- Vanilla JavaScript (SPA-szerű, 1250+ sor)
- Egyedi CSS (dark theme)
- SessionStorage alapú auth kezelés

**Infrastruktúra:**
- Docker + Docker Compose
- Oracle Cloud Always Free ARM VM (2 OCPU, 12 GB RAM)
- MySQL 8.4 (külön DB séma szolgáltatásonként)
- Prometheus + Grafana (metrikák, dashboard)
- ELK Stack (Elasticsearch, Logstash, Kibana) — opcionális
- Logstash Logback Encoder (strukturált naplózás)

**CI/CD:**
- GitHub Actions
- Multi-arch image build (linux/amd64 + linux/arm64)
- GHCR (GitHub Container Registry)
- Automatikus build push után

## Gyors indulás

### Előfeltételek
- Java 17+
- Maven 3.8+
- Docker + Docker Compose

### Lokális fejlesztés

```bash
# Repo klónozása
git clone https://github.com/kenyereskrisztian/Scalable_E-Commerce_Platform.git
cd Scalable_E-Commerce_Platform

# Docker Compose indítás (db + ELK nélkül, fejlesztéshez)
docker compose -f docker-compose.dev.yml up -d

# Build és futtatás (egy szolgáltatás, pl. user-service)
cd user-service && mvn spring-boot:run
```

### Éles (Oracle Cloud VPS)

```bash
# Automatikus telepítés (Docker, ufw, .env, image-ek)
sudo bash scripts/vps-deploy.sh

# Frissítés
bash scripts/vps-deploy.sh update
```

Részletes útmutató: [`docs/VPS-deploy.md`](docs/VPS-deploy.md)

## Monitoring

| Eszköz | Elérhetőség | Funkció |
|---|---|---|
| **Grafana** | SSH tunnel → `localhost:3000` | Dashboard, metrikák vizualizálása |
| **Prometheus** | SSH tunnel → `localhost:9090` | Idősoros metrikák gyűjtése |
| **Kibana** | SSH tunnel → `localhost:5601` | Strukturált naplók keresése (ELK) |

```bash
# SSH tunnel a monitoring UI-khoz
ssh -i ~/.ssh/oracle_key -L 3000:localhost:3000 -L 5601:localhost:5601 ubuntu@<PUBLIC_IP>
```

> **Megjegyzés:** Az ELK stack alapból nem indul (erőforrás-igényes).
> Manuális indítás: `docker compose --profile elk up -d`

## Kubernetes

A `k8s/` mappában minden szolgáltatás Kubernetes manifesztje megtalálható:
- Deployment + Service every microservice
- MySQL StatefulSet + PVC
- Namespace isolation
- HPA (Horizontal Pod Autoscaler)

Részletes útmutató: [`docs/Kubernetes-deploy.md`](docs/Kubernetes-deploy.md)

## Projektszerkezet

```
Scalable_E-Commerce_Platform/
├── api-gateway/            # Spring Cloud Gateway
├── cart-service/           # Kosár kezelés
├── common/                 # Megosztott kód (JwtUtil, DTO-k)
├── discovery-service/      # Netflix Eureka
├── docs/                   # Dokumentáció
├── frontend-service/       # Tesztfelület (JS + CSS)
├── k8s/                    # Kubernetes manifesztok
├── mysql/                  # Dockerfile + seed.sql
├── notification-service/   # Értesítések
├── order-service/          # Rendelések
├── payment-service/        # Fizetések
├── product-service/        # Termékek + kategóriák
├── scripts/                # Deploy scriptek, configok
├── user-service/           # Felhasználók + auth
├── docker-compose.yml      # Production compose (Oracle VPS)
├── docker-compose.dev.yml  # Development compose
└── pom.xml                 # Maven parent (multi-module)
```

## API végpontok

| Method | Útvonal | Leírás |
|---|---|---|
| `POST` | `/api/auth/register` | Regisztráció |
| `POST` | `/api/auth/login` | Bejelentkezés (JWT) |
| `GET` | `/api/products` | Termékek listázása |
| `POST` | `/api/products` | Termék létrehozása |
| `GET` | `/api/categories` | Kategóriák listázása |
| `POST` | `/api/categories` | Kategória létrehozása |
| `GET` | `/api/cart` | Kosár tartalma |
| `POST` | `/api/cart/add` | Termék hozzáadása |
| `POST` | `/api/orders` | Rendelés leadása |
| `POST` | `/api/payments/{orderId}` | Fizetés indítása |
| `POST` | `/api/notifications/send` | Értesítés küldése |

> Minden végpont (kivéve auth) JWT Bearer token-t igényel az `Authorization` header-ben.

## Fejlesztő

**Kenyeres Krisztián** — [GitHub](https://github.com/kenyereskrisztian)
