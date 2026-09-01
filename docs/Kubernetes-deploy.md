# Kubernetes — Megvalósítási Terv és Használati Útmutató (Phase 6.3)

Kubernetes manifest-készlet a teljes platformhoz: namespace, MySQL, 8 Spring Boot microservice
(mindegyik saját almappában: Deployment + Service + szükség esetén ConfigMap) és a frontend.

> **ÁLLAPOT (2026-09):** a manifestek elkészültek, élesteszteltek és commitolva (`cbc0c79`). A telepítés
> a `scripts/kubectl.sh` scripttel történik. Minden service fut a clusteren; a frontend UI a
> service-eket zölddel jelzi. **Nincs éles (production) deploy — a manifestek a saját/teszt clusterre
> készültek.**

## Döntések

| Kérdés | Döntés |
|---|---|
| Névtér | Egyetlen `ecommerce` namespace |
| Image-ek | GHCR (`ghcr.io/kenyereskrisztian/<service>:latest`), privát → `ghcr-secret` imagePullSecret |
| Titkok | **Nem** commitolunk secret-et. Két kézzel létrehozott secret-re hivatkozunk (`ecommerce-shared-secret`, `ghcr-secret`) |
| ConfigMapek | Service-enkénti (`<service>-configmap`), központi `01-configmap.yaml` helyett |
| Külső elérés | Csak a frontend kap Ingress-t (`frontend.local`); az API-k a gateway-en (ClusterIP) mennek át, külön Ingress nélkül |
| Skálázás | `frontend-service` 2 replica, a többi 1 (a resource-keret a user-service mintáját követi) |

## Felépítés

```
k8s/
├── 00-namespace.yaml            # ecommerce névtér
├── 03-mysql.yaml                # ConfigMap (seed) + PVC + Deployment + Service (3306)
├── discovery-service/           # Eureka (8761) — Deployment + Service
├── api-gateway/                 # Spring Cloud Gateway (8080) — Deployment + Service + ConfigMap
├── user-service/                # 8081, DB: ecommerce_users
├── product-service/             # 8082, DB: ecommerce_products
├── cart-service/                # 8083, DB: ecommerce_cart
├── order-service/               # 8084, DB: ecommerce_orders
├── payment-service/             # 8085, nincs DB
├── notification-service/        # 8086, nincs DB
└── frontend-service/            # 5500, 2 replica + Ingress (frontend.local)
```

Minden service a `ecommerce` névtérben él, a cluster-DNS-en keresztül éri el egymást
(`discovery-service`, `mysql`), a gateway Eureka-n keresztül (`lb://<service>`) továbbít.

## Titkok (kézzel hozd létre egyszer)

A démarfájlok **referálják**, de nem tartalmazzák a titkokat. Első alkalommal hozd létre őket:

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<git-user> \
  --docker-password=<GITHUB_TOKEN/AccessToken> \
  -n ecommerce

kubectl create secret generic ecommerce-shared-secret \
  --from-literal=MYSQL_PASSWORD=<root jelszó> \
  --from-literal=JWT_SECRET=<a .env-ből vett JWT_SECRET> \
  -n ecommerce
```

> **Fontos:** a MySQL is innen veszi a root jelszót (`MYSQL_ROOT_PASSWORD`), a service-ek pedig a
> `MYSQL_PASSWORD`-t, ezért a két értéknek meg kell egyeznie a seed adatbázisokhoz.

## Service-enkénti ConfigMap-ek

| ConfigMap | Kulcsok |
|---|---|
| `<db-service>-configmap` (user/product/cart/order) | `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `DB_NAME`, `EUREKA_DEFAULT_ZONE`, `LOGSTASH_URL` |
| `api-gateway-configmap` | `EUREKA_DEFAULT_ZONE` |
| `payment/notification-configmap` | `EUREKA_DEFAULT_ZONE`, `LOGSTASH_URL` |
| `discovery-service` | nincs (önálló) |

`EUREKA_DEFAULT_ZONE` mindenhol: `http://discovery-service:8761/eureka/`.

## Deployment minták

- **`startupProbe`** (`httpGet /actuator/health`, `failureThreshold: 30`, `periodSeconds: 10`)
  → 5 perc indítási türelem. A Spring/Eureka service-ek 60–90 mp alatt indulnak; enélkül a
  liveness probe megöli őket crash-loopban.
- **readiness/liveness** a megfelelő porton (`/actuator/health`).
- **resource-keret:** requests 256Mi/250m, limits 512Mi/500m. (Kötelező a CPU-alapú HPA-hoz —
  a frontend is kapott request/limit keretet.)
- **imagePullSecrets:** `ghcr-secret` minden service-nél.
- **envFrom:** `<service>-configmap` + `ecommerce-shared-secret`.

## HPA — horizontális skálázás

Minden service-hez saját `hpa.yaml` (9 db). CPU-alapú, a metrics-server küszöbértéke alapján:

| Szolgáltatás | minReplicas | maxReplicas | cél (CPU) |
|---|---|---|---|
| frontend-service | 2 | 3 | 75% |
| többi 8 service | 1 | 3 | 75% |

> **Előfeltétel:** CPU-alapú HPA-hoz futnia kell a **metrics-servernek** a clusteren
> (`kubectl top nodes` működjön). Enélkül az HPA "unknown" targetet mutat, és nem skáláz.

## Telepítés

```bash
./scripts/kubectl.sh            # apply, dependencies-first sorrendben
./scripts/kubectl.sh --clean    # előbb törli a 9 deploymentet, majd újraapply
```

A script sorrendje: namespace → MySQL → discovery → user → product → cart → order →
payment → notification → api-gateway → frontend; minden lépés után `kubectl rollout status`
(300 mp timeout) ellenőriz. A HPA-k (`k8s/<service>/hpa.yaml`) a `-R` apply-val szintén
felkerülnek, a `--clean` törli a HPA-kat is; a végén `kubectl get hpa` listázza őket.

Skálázás ellenőrzése:

```bash
kubectl get hpa -n ecommerce                 # TARGETS: <aktuális>%/75%
kubectl top pods -n ecommerce                # tényleges CPU/memória (metrics-server kell hozzá)
```

## Tesztelés

```bash
# discovery-s elérés — Eureka dashboard, hány service regisztrált
kubectl port-forward svc/discovery-service -n ecommerce 8761:8761

# gateway — API-hívások
kubectl port-forward svc/api-gateway -n ecommerce 8080:80
curl -i http://localhost:8080/api/products                  # 200, token nélkül is
curl -i http://localhost:8080/api/cart                      # 401 (token nélkül)

# frontend — UI
kubectl port-forward svc/frontend-service -n ecommerce 5500:80
# vagy Ingress-szel: http://frontend.local (hosts bejegyzés szükséges)
```

## Ismert buktatók (megoldva)

1. **`discovery-service` Service portja 8761 legyen** (`port: 8761`, `targetPort: 8761`), NE 80-as.
   Ellenkező esetben a service-ek `http://discovery-service:8761`-re `Connection refused`-öt
   kapnak, semmi nem regisztrál a Eureka-n, a gateway 503-at ad.
2. **Lassú indítás → legyen `startupProbe`**, különben a liveness probe megöli a podot,
   mielőtt befejezné az indulást (crash-loop). Lásd a Deployment mintákat.

## Érintett fájlok

- `k8s/*` — manifestek (namespace, mysql, 8 service, frontend + ingress)
- `scripts/kubectl.sh` — telepítő script (`--clean` flag)
- `docs/PLAN.md` — 6.3 pont