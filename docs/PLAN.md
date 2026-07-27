# Fejlesztési Terv — Scalable E-Commerce Platform

## Phase 1 — Monolit MVP
Egyetlen Spring Boot alkalmazás, minden funkció egyben.

- [X] 1.1 Model layer — `User`, `Product`, `Category`, `CartItem`, `Order`, `OrderItem` entitások JPA-val
- [ ] 1.2 Repository layer — Spring Data JPA repository-k
- [ ] 1.3 Service layer — üzleti logika (regisztráció/login JWT-vel, termék CRUD, kosárkezelés, rendelés leadás)
- [ ] 1.4 Controller layer — REST API végpontok
- [ ] 1.5 DTO + mapper — kérés/válasz objektumok, mapping
- [ ] 1.6 Exception handling — `@ControllerAdvice` globális hibakezelés
- [ ] 1.7 Security — Spring Security + JWT autentikáció
- [ ] 1.8 Tesztek — repository/service unit tesztek

**Eredmény:** Működő REST API, Postmannal tesztelhető.

---

## Phase 2 — Multi-Module Microservices
Monolit szétbontása független Maven modulokra.

- [ ] 2.1 Parent POM átalakítása multi-module-ra
- [ ] 2.2 `common` modul — shared DTO-k, util-ok
- [ ] 2.3 `user-service` kiemelése (port: 8081)
- [ ] 2.4 `product-service` kiemelése (port: 8082)
- [ ] 2.5 `cart-service` kiemelése (port: 8083)
- [ ] 2.6 `order-service` kiemelése (port: 8084)
- [ ] 2.7 `payment-service` kiemelése (port: 8085)
- [ ] 2.8 `notification-service` kiemelése (port: 8086)
- [ ] 2.9 Szolgáltatások közötti REST kommunikáció (WebClient vagy OpenFeign)

**Eredmény:** 6 külön Spring Boot alkalmazás, egymást REST-en hívják.

---

## Phase 3 — Infrastruktúra (Discovery + Gateway)
Service discovery és API gateway a rendszer elé.

- [ ] 3.1 `discovery-service` — Eureka Server (port: 8761)
- [ ] 3.2 Minden service Eureka Client + `@EnableDiscoveryClient`
- [ ] 3.3 `api-gateway` — Spring Cloud Gateway (port: 8080)
- [ ] 3.4 Gateway route-ok (/api/users/** → user-service, stb.)
- [ ] 3.5 Gateway szintű JWT validation (centralizált auth)
- [ ] 3.6 Service discovery-s névre áttérés szolgáltatások között

**Eredmény:** Egyetlen endpoint (localhost:8080), Gateway-en keresztül.

---

## Phase 4 — Docker + DevOps
Konténerizáció és lokális infrastruktúra.

- [ ] 4.1 Dockerfile minden service-hez (multi-stage build)
- [ ] 4.2 `docker-compose.yml` — MySQL + minden service + Eureka
- [ ] 4.3 `docker-compose.override.yml` dev profilhoz (hot reload)
- [ ] 4.4 `.env` fájl környezeti változóknak

**Eredmény:** `docker-compose up --build` → teljes rendszer fut.

---

## Phase 5 — Monitoring + Centralized Logging
- [ ] 5.1 `spring-boot-starter-actuator` minden service-ben
- [ ] 5.2 Prometheus + Grafana docker-compose-ba
- [ ] 5.3 ELK Stack (Elasticsearch, Logstash, Kibana)
- [ ] 5.4 Health check + metrics endpointok

---

## Phase 6 — CI/CD Pipeline
- [ ] 6.1 GitHub Actions: build + test PR-nként
- [ ] 6.2 Docker image build + push registry-be
- [ ] 6.3 Deploy script (Docker Swarm vagy Kubernetes manifestek)
