# Eureka Service Discovery — Megvalósítási Terv (Phase 3.1, 3.2, 3.6)

A `spring-cloud.version` property fix kész (pom.xml:35). Ebből a tervből lehet dolgozni.

> **ÁLLAPOT (2026-08):** a terv minden lépése kész és be van építve. A 3.6-ot (név alapú
> hívások) a `cart-service`, `order-service` és `payment-service` WebClient-kliensei már
> használják (`http://<service-name>`, `@LoadBalanced` builder). Az `api-gateway` is
> regisztrál a Eureka-ba és `lb://` route-okkal továbbít. Ellenőrzés: `scripts/start-all.bat`,
> Eureka dashboard: http://localhost:8761.

## Felépítés

```
                    ┌─────────────────────────────┐
                    │  discovery-service (Eureka) │   port: 8761
                    └──────────────┬──────────────┘
        ┌───────────┬──────────────┼──────────────┬───────────┐
        │           │              │              │           │
   user-service product-service cart-service order-service payment-service notification-service
     (8081)          (8082)         (8083)        (8084)       (8085)         (8086)
```

Minden service regisztrálja magát az Eureka szervernél (a `spring.application.name` alapján),
és a többi service-t **név alapján** hívhatja (`http://PRODUCT-SERVICE/...`) a hardcoded
`localhost:PORT` helyett.

---

## 1. lépés — `discovery-service` modul létrehozása

### 1.1 Parent pom: modul felvétele

`pom.xml` `<modules>` blokk (pom.xml:21-29) → a `common` elé vagy után, sorrend nem számít:

```xml
<module>discovery-service</module>
```

### 1.2 `discovery-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>scalable-ecommerce-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>discovery-service</artifactId>
    <name>Discovery Service</name>
    <description>Eureka Server for service discovery</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
    </dependencies>
</project>
```

A verziót a parent `dependencyManagement`-je adja (a `spring-cloud-dependencies` BOM),
ezért nincs `<version>`.

### 1.3 Main osztály

`discovery-service/src/main/java/com/ecommerce/discoveryservice/DiscoveryServiceApplication.java`

```java
package com.ecommerce.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
```

### 1.4 `discovery-service/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: discovery-service

server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

`register-with-eureka: false` + `fetch-registry: false` → **standalone** mód: a szerver nem
próbálja magát regisztrálni. `enable-self-preservation: false` fejlesztésnél kényelmes,
élesben érdemes visszakapcsolni.

---

## 2. lépés — Kliens oldal (a 6 service)

Minden szerviznél ugyanaz a 3 módosítás:

### 2.1 Dependency a service pom-jába

Minden `*/pom.xml` `<dependencies>` blokkjába:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 2.2 `application.yml` kiegészítés (minden service-nél)

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

A `spring.application.name` már mindenhol megvan (ez lesz a regisztrált név: `user-service`, stb.).

### 2.3 Main osztály

**Nem kell** `@EnableDiscoveryClient` / `@EnableEurekaClient`: a Spring Cloud 2023.0.x-ben
ezek deprecated-ek, a classpath-on lévő eureka-client automatikus konfigurációt indít.

---

## 3. lépés (opcionális, 3.6) — Név alapú hívások

Jelenleg a WebClient-ek hardcoded URL-t használnak (pl. `cart-service/.../ProductServiceClient.java:14`):

| Fájl | Jelenlegi URL |
|---|---|
| `cart-service/.../client/ProductServiceClient.java` | `http://localhost:8082` |
| `cart-service/.../client/UserServiceClient.java` | `http://localhost:8081` |
| `order-service/.../client/ProductServiceClient.java` | `http://localhost:8082` |
| `order-service/.../client/CartServiceClient.java` | `http://localhost:8083` |
| `order-service/.../client/UserServiceClient.java` | `http://localhost:8081` |
| `payment-service/.../client/OrderServiceClient.java` | `http://localhost:8084` |

Átállás a discovery-s névre:

1. **LoadBalancer dependency** minden hívó service pom-jába:
   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-loadbalancer</artifactId>
   </dependency>
   ```

2. **`WebClient.Builder` bean** (`@LoadBalanced`), pl. minden service-ben egy `WebClientConfig`:
   ```java
   @Configuration
   public class WebClientConfig {
       @Bean
       @LoadBalanced
       public WebClient.Builder webClientBuilder() {
           return WebClient.builder();
       }
   }
   ```

3. **Client átírás** — a `WebClient.create("http://localhost:8082")` helyett injektáld a
   builder-t és a service nevet használd (a regisztrált név = `spring.application.name`):
   ```java
   public ProductServiceClient(WebClient.Builder builder) {
       this.webClient = builder.baseUrl("http://product-service").build();
   }
   ```
   (Eureka-ban a nevek nagybetűs formában is elérhetők: `PRODUCT-SERVICE`.)

---

## Ellenőrzés

```bash
mvn clean install          # az egész workspace buildel
mvn spring-boot:run -pl discovery-service
# http://localhost:8761 → Eureka dashboard
```

Ezután indítsd a service-eket: a dashboard "Instances currently registered" szekciójában
megjelennek (`USER-SERVICE`, `PRODUCT-SERVICE`, ...). Név alapú hívásoknál ellenőrizd,
hogy pl. a cart-service le tudja kérni a product-service termékét a 
`POST /api/cart/items` hívással.

## Érintett fájlok összefoglalása

**Új:**
- `discovery-service/pom.xml`
- `discovery-service/src/main/java/com/ecommerce/discoveryservice/DiscoveryServiceApplication.java`
- `discovery-service/src/main/resources/application.yml`

**Módosítandó:**
- `pom.xml` (module felvétele)
- 6 × service pom (`eureka-client` dependency)
- 6 × `application.yml` (`eureka.client.service-url.defaultZone`)
- 6 × WebClient client osztály (ha a 3.6-ot is csináljuk)
- 3 × service pom (`loadbalancer` dependency, ha a 3.6-ot is csináljuk)
