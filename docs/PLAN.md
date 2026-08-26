# Fejlesztési Terv — Scalable E-Commerce Platform

## Phase 1 — Monolit MVP
Egyetlen Spring Boot alkalmazás, minden funkció egyben.

- [X] 1.1 Model layer — `User`, `Product`, `Category`, `CartItem`, `Order`, `OrderItem` entitások JPA-val
- [X] 1.2 Repository layer — Spring Data JPA repository-k
- [X] 1.3 Service layer — üzleti logika (regisztráció/login, termék CRUD, kosárkezelés, rendelés leadás)
- [X] 1.4 Controller layer — REST API végpontok
- [X] 1.5 DTO — kérés/válasz objektumok
- [X] 1.6 Exception handling — `@ControllerAdvice` globális hibakezelés
- [X] 1.7 Security — Spring Security + JWT autentikáció
- [X] 1.8 Tesztek — repository/service/controller unit tesztek H2 + Mockito

**Eredmény:** Működő REST API, Postmannal tesztelhető.

---

## Phase 2 — Multi-Module Microservices
Monolit szétbontása független Maven modulokra.

- [X] 2.1 Parent POM átalakítása multi-module-ra
- [X] 2.2 `common` modul — shared DTO-k, util-ok
- [X] 2.3 `user-service` kiemelése (port: 8081)
- [X] 2.4 `product-service` kiemelése (port: 8082)
- [X] 2.5 `cart-service` kiemelése (port: 8083)
- [X] 2.6 `order-service` kiemelése (port: 8084)
- [X] 2.7 `payment-service` kiemelése (port: 8085)
- [X] 2.8 `notification-service` kiemelése (port: 8086)
- [X] 2.9 Szolgáltatások közötti REST kommunikáció (WebClient)

**Eredmény:** 6 külön Spring Boot alkalmazás, egymást REST-en hívják.

**Eredmény:** 6 külön Spring Boot alkalmazás, egymást REST-en hívják.

---

## Phase 3 — Infrastruktúra (Discovery + Gateway)
Service discovery és API gateway a rendszer elé.

- [X] 3.1 `discovery-service` — Eureka Server (port: 8761)
- [X] 3.2 Minden service Eureka Client + `@EnableDiscoveryClient` (a 2023.0.x-ben a client dependency automatikusan regisztrál, `@EnableDiscoveryClient` nem kell)
- [X] 3.3 `api-gateway` — Spring Cloud Gateway (port: 8080)
- [X] 3.4 Gateway route-ok (/api/users/** → user-service, stb.)
- [X] 3.5 Gateway szintű JWT validation (centralizált auth)
- [X] 3.6 Service discovery-s névre áttérés szolgáltatások között (cart/order/payment WebClient `http://<service-name>`)

**Eredmény:** Egyetlen endpoint (localhost:8080), Gateway-en keresztül.

---

## Phase 4 — Docker + DevOps
Konténerizáció és lokális infrastruktúra.

- [x] 4.1 Dockerfile minden service-hez (multi-stage build)
- [x] 4.2 `docker-compose.yml` — MySQL + minden service + Eureka
- [x] 4.3 `docker-compose.dev.yml` dev profilhoz (hot reload)
- [x] 4.4 `.env` fájl környezeti változóknak

**Eredmény:** `docker-compose up --build` → teljes rendszer fut.

---

## Phase 5 — Monitoring + Centralized Logging
- [x] 5.1 `spring-boot-starter-actuator` minden service-ben
- [x] 5.2 Prometheus + Grafana docker-compose-ba
- [ ] 5.3 ELK Stack (Elasticsearch, Logstash, Kibana)
- [ ] 5.4 Health check + metrics endpointok

---

## Phase 6 — CI/CD Pipeline
- [ ] 6.1 GitHub Actions: build + test PR-nként
- [ ] 6.2 Docker image build + push registry-be
- [ ] 6.3 Deploy script (Docker Swarm vagy Kubernetes manifestek)
