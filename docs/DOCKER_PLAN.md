# Docker + DevOps — Megvalósítási Terv (Phase 4)

A rendszer konténerizálása: minden service-hez multi-stage Dockerfile, egy `docker-compose.yml`
a teljes stack-hez (MySQL + Eureka + 6 service + gateway), és env-alapú konfiguráció.

> **ÁLLAPOT (2026-08):** a terv elkészült, **megvalósítva** — mind a 8 service Dockerfile-ja
> kész, a compose futtatja a teljes stacket, a smoke teszt (Eureka 7/7, gateway flow) sikeres.
> Az aktuális `docker-compose.yml` némileg eltér a sablontól (nincs discovery healthcheck,
> `service_started` a discovery függőségnél), de funkcióban azonos.

## Döntések (felhasználóval egyeztetve)

| Kérdés | Döntés |
|---|---|
| Hol fut a Docker? | **Windows PowerShell** (`docker compose` a `C:\Users\kenye\Documents\java\Scalable_E-Commerce_Platform` mappában) |
| Frontend? | **Marad host-on** (python http.server, `localhost:5500`), nincs frontend konténer |
| Előző munka commitolva? | Igen, `b002cff` |

## Felépítés

```
        ┌─────────────── docker-compose.yml ───────────────┐
        │                                                  │
        │   db (mysql:8.4, seed.sql az initdb-be, volume)  │
        │   discovery-service  → host 8761                 │
        │   user-service       → host 8081 (db-től függ)   │
        │   product-service    → host 8082 (db-től függ)   │
        │   cart-service       → host 8083 (db-től függ)   │
        │   order-service      → host 8084 (db-től függ)   │
        │   payment-service    → host 8085                 │
        │   notification-service → host 8086               │
        │   api-gateway        → host 8080 (discovery-től függ) │
        │                                                  │
        │   Frontend: HOST (localhost:5500) ──→ :8080      │
        └──────────────────────────────────────────────────┘
```

Minden service a **compose hálózaton** kommunikál service-névvel (`db`, `discovery-service`).
A gatewey a `lb://` route-okkal a Eureka-n keresztül továbbít.

---

## 0. lépés — Előfeltétel: WSL folyamatok leállítása

A WSL-en most fut a 8 service + MariaDB. A WSL2 localhost-forwarding miatt ezek **ütköznek**
a Windows localhost portokkal (8761, 8080–8086, 3306), amiket a Docker Desktop publikál.

Leállítás WSL-ből:

```bash
# a futó service-ek PID-jei: discovery 8761, user 8081, product 8082, cart 8083,
# order 8084, payment 8085, notification 8086, gateway 8080
for p in <service-pid-ek>; do kill "$p"; done
# MariaDB (MB=/tmp/opencode/tools/mariadb-10.11.18-linux-systemd-x86_64)
$MB/bin/mariadb-admin -uroot -ptest1234 shutdown
```

Ellenőrzés: `ss -ltnp | grep -E '8761|808[0-6]|3306'` → üres.

---

## 1. lépés — Konfiguráció env-be (7 `application.yml`)

Cél: **Spring placeholder defaulttal**, így a helyi `scripts/start-all.bat` változatlanul
működik (a defaultok a mostani értékek), Dockerben pedig a compose env-je felülírja.

Módosítandó fájlok: `user-service`, `product-service`, `cart-service`, `order-service`,
`payment-service`, `notification-service`, `api-gateway` → `src/main/resources/application.yml`.

### 1.1 Adatbázis (a 4 DB-s service)

```yaml
spring.datasource:
  url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${DB_NAME:ecommerce_users}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
  username: ${MYSQL_USER:root}
  password: ${MYSQL_PASSWORD:test1234}
```

`DB_NAME` default service-enként: `ecommerce_users` / `ecommerce_products` / `ecommerce_cart`
/ `ecommerce_orders`.

### 1.2 Eureka (mind a 7 service)

**Kritikus Docker szempont:** a `eureka.instance.hostname: localhost` konténerben eltöri a
név-alapú hívást (a kliens a saját `localhost`-ját próbálná elérni). Helyette IP-alapú regisztráció:

```yaml
eureka:
  instance:
    prefer-ip-address: true
  client:
    service-url:
      defaultZone: ${EUREKA_DEFAULT_ZONE:http://localhost:8761/eureka/}
```

A `hostname: localhost` sor **törlendő** mindenhonnan. `prefer-ip-address: true` helyileg is
működik (LAN IP regisztráció).

### 1.3 JWT titok (mind a 7 service)

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:NOvxdE/9L4Mn4dKRSzIcGNnHjf3gdtdTopmti0n7Dy4=}
    expiration: 86400000
```

---

## 2. lépés — Dockerfile / service (9 db)

Multi-stage build: Maven a buildhez, JRE 17 a futáshoz. A `-am` biztosítja, hogy a `common`
modul is felépüljön.

Minta — `user-service/Dockerfile` (a többi 8 azonos szerkezetű, csak a modulnév és port más):

```dockerfile
# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common common
COPY user-service user-service
RUN mvn -B -pl user-service -am -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/user-service/target/*.jar app.jar
EXPOSE 8081
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx256m"
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Modulonkénti adatok (Dockerfile-ban `EXPOSE` + `COPY ... <modul>/target/*.jar`):

| Service | Modul | Port | `COPY` utasítás |
|---|---|---|---|
| discovery | `discovery-service` | 8761 | `COPY discovery-service discovery-service` |
| gateway | `api-gateway` | 8080 | `COPY api-gateway api-gateway` + `COPY common common` |
| user | `user-service` | 8081 | `COPY user-service user-service` + `COPY common common` |
| product | `product-service` | 8082 | ... |
| cart | `cart-service` | 8083 | ... |
| order | `order-service` | 8084 | ... |
| payment | `payment-service` | 8085 | ... |
| notification | `notification-service` | 8086 | ... |

A `common`-ot copyzó fájloknak a `discovery-service` kivételével mindnek copyzniuk kell a
`common` mappát (az 6 business service + gateway függ tőle). A `discovery-service` önálló.

---

## 3. lépés — `docker-compose.yml` + `.env`

### 3.1 `docker-compose.yml` (projekt gyökér)

```yaml
services:
  db:
    image: mysql:8.4
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD:-test1234}
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./scripts/seed.sql:/docker-entrypoint-initdb.d/seed.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-ptest1234"]
      interval: 5s
      timeout: 3s
      retries: 20

  discovery-service:
    build: { context: ., dockerfile: discovery-service/Dockerfile }
    ports: ["8761:8761"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/eureka/apps"]
      interval: 5s
      timeout: 3s
      retries: 20

  user-service:
    build: { context: ., dockerfile: user-service/Dockerfile }
    ports: ["8081:8081"]
    depends_on:
      db: { condition: service_healthy }
      discovery-service: { condition: service_healthy }
    environment:
      MYSQL_HOST: db
      MYSQL_PORT: "3306"
      DB_NAME: ecommerce_users
      MYSQL_USER: root
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-test1234}
      EUREKA_DEFAULT_ZONE: http://discovery-service:8761/eureka/
      JWT_SECRET: ${JWT_SECRET}

  # product-service (8082, ecommerce_products), cart-service (8083, ecommerce_cart),
  # order-service (8084, ecommerce_orders) — azonos sablon, saját DB_NAME és port

  payment-service:
    build: { context: ., dockerfile: payment-service/Dockerfile }
    ports: ["8085:8085"]
    depends_on:
      discovery-service: { condition: service_healthy }
    environment:
      EUREKA_DEFAULT_ZONE: http://discovery-service:8761/eureka/
      JWT_SECRET: ${JWT_SECRET}

  notification-service:
    build: { context: ., dockerfile: notification-service/Dockerfile }
    ports: ["8086:8086"]
    depends_on:
      discovery-service: { condition: service_healthy }
    environment:
      EUREKA_DEFAULT_ZONE: http://discovery-service:8761/eureka/
      JWT_SECRET: ${JWT_SECRET}

  api-gateway:
    build: { context: ., dockerfile: api-gateway/Dockerfile }
    ports: ["8080:8080"]
    depends_on:
      discovery-service: { condition: service_healthy }
    environment:
      EUREKA_DEFAULT_ZONE: http://discovery-service:8761/eureka/
      JWT_SECRET: ${JWT_SECRET}

volumes:
  mysql-data:
```

Jegyzetek:
- `seed.sql` a `/docker-entrypoint-initdb.d/`-be mountolva → **első indításkor** lefut
  (a `CREATE DATABASE IF NOT EXISTS` sorok létrehozzák a 4 DB-t), volume-tal perzisztens.
- `eureka.instance.prefer-ip-address: true` miatt a service-ek a konténer IP-jüket regisztrálják,
  a `lb://` hívások a compose hálózaton mennek.
- DB-s service-eknek `depends_on` a `db`-re is (`service_healthy`), nem csak a discovery-re!

### 3.2 `.env` (projekt gyökér, **gitignored**) + `.env.example` (commitolva)

`.env.example`:
```
MYSQL_PASSWORD=test1234
JWT_SECRET=NOvxdE/9L4Mn4dKRSzIcGNnHjf3gdtdTopmti0n7Dy4=
```

`.gitignore` kiegészítés: `.env`

---

## 4. lépés (opcionális, PLAN.md 4.3) — dev override

`docker-compose.override.yml` a hot reloadhoz (később, ha kell):
- service portok host-megosztása (már az alaptervben benne van),
- source bind-mount + `spring-boot-devtools` dependency hozzáadása a service-ekhez
  (a `devtools` önálló `optional` függőség, prod image-ből kimarad).

Nem kötelező a 4.1–4.2-höz; a Phase 4 fő célja a `docker-compose up --build` → teljes rendszer.

---

## Ellenőrzés (PowerShell, projekt mappa)

```powershell
docker compose config          # yml + env ellenőrzés
docker compose up -d --build   # build + indítás
docker compose ps              # mindegyik "Up (healthy)"
docker ps

# Eureka: 7/7 regisztráció a dashboardon
curl.exe http://localhost:8761

# Gateway flow (ugyanaz, mint WSL-en):
curl.exe http://localhost:8080/api/products                 # 200, token nélkül is
curl.exe http://localhost:8080/api/cart                     # 401 (token nélkül)
curl.exe -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"...","password":"..."}'
# a kapott token-tel: /api/cart → 200
```

## Érintett fájlok összefoglalása

**Új:**
- `discovery-service/Dockerfile`, `api-gateway/Dockerfile`, `user-service/Dockerfile`,
  `product-service/Dockerfile`, `cart-service/Dockerfile`, `order-service/Dockerfile`,
  `payment-service/Dockerfile`, `notification-service/Dockerfile`
- `docker-compose.yml`, `.env.example`
- `.env` (gitignored)

**Módosítandó:**
- 7 × `src/main/resources/application.yml` (env placeholderek + `prefer-ip-address: true`)
- `.gitignore` (`.env` hozzáadása)

**Nem érintett:**
- `frontend/` (host-on marad), `scripts/start-all.bat` (defaultok miatt tovább működik)

**Dokumentáció:**
- `docs/PLAN.md`: a 4.1, 4.2, 4.4 pontok pipálása sikeres verifikáció után (4.3 opcionális).
