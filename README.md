# Scalable_E-Commerce_Platform
Valami random projekt
Skálázható webáruház fejlesztése mikroszolgáltatás-architektúrával és Dockerrel

A feladat célja egy skálázható webáruház (e-commerce platform) létrehozása mikroszolgáltatás-architektúra és Docker segítségével.

Az alkalmazás egy online áruház fő funkcióit valósítja meg, például:

termékkatalógus kezelése,
felhasználók regisztrációja és bejelentkezése,
bevásárlókosár kezelése,
fizetések feldolgozása,
rendelések kezelése.

Minden funkció külön mikroszolgáltatásként működik, így azok egymástól függetlenül fejleszthetők, telepíthetők és skálázhatók.

Alapvető mikroszolgáltatások
1. Felhasználókezelő szolgáltatás (User Service)

Feladata:

felhasználói regisztráció,
bejelentkezés (autentikáció),
felhasználói profilok kezelése.
2. Termékkatalógus szolgáltatás (Product Catalog Service)

Feladata:

termékek kezelése,
kategóriák kezelése,
készletnyilvántartás.
3. Bevásárlókosár szolgáltatás (Shopping Cart Service)

Feladata:

termék hozzáadása a kosárhoz,
termék eltávolítása,
darabszám módosítása,
a felhasználó aktuális kosarának kezelése.
4. Rendeléskezelő szolgáltatás (Order Service)

Feladata:

rendelések leadása,
rendelési állapot követése,
rendelési előzmények tárolása.
5. Fizetési szolgáltatás (Payment Service)

Feladata:

fizetések feldolgozása,
külső fizetési szolgáltatók (például Stripe vagy PayPal) integrálása.
6. Értesítési szolgáltatás (Notification Service)

Feladata:

e-mail értesítések küldése,
SMS értesítések küldése.

Például:

rendelés visszaigazolása,
csomag feladása,
kiszállítás állapota.

Ehhez használhatók külső szolgáltatások, például:

Twilio
SendGrid
Kiegészítő komponensek

A rendszer megbízhatóságának és skálázhatóságának növelésére az alábbi elemek is beépíthetők.

API Gateway

Az API Gateway az alkalmazás központi belépési pontja.

Feladata:

minden klienskérés fogadása,
a kérések továbbítása a megfelelő mikroszolgáltatáshoz,
szükség esetén hitelesítés vagy terheléselosztás.

Használható megoldások:

Kong
Traefik
NGINX
Service Discovery

Automatikusan nyilvántartja és felderíti a futó mikroszolgáltatásokat.

Ennek segítségével a szolgáltatások IP-cím vagy port ismerete nélkül is megtalálják egymást.

Lehetséges eszközök:

Consul
Eureka
Központi naplózás (Centralized Logging)

Minden mikroszolgáltatás naplóit egy helyre gyűjti.

Előnyei:

egyszerűbb hibakeresés,
könnyebb monitorozás,
gyorsabb problémamegoldás.

Ajánlott technológia:

ELK Stack
Elasticsearch
Logstash
Kibana
Docker és Docker Compose

Minden mikroszolgáltatás külön Docker konténerben fut.

A Docker Compose segítségével:

egyszerre indíthatók,
hálózatba köthetők,
konfigurálhatók,
egyszerűen kezelhetők.
CI/CD Pipeline

Automatizálja:

a fordítást,
a tesztelést,
a telepítést.

Használható eszközök:

Jenkins
GitLab CI
GitHub Actions
Fejlesztési lépések
1. Docker környezet kialakítása
Docker telepítése.
Dockerfile készítése minden mikroszolgáltatáshoz.
Docker Compose konfigurálása.
2. Mikroszolgáltatások fejlesztése

Érdemes először egy MVP-t (Minimum Viable Product) elkészíteni minden szolgáltatásból, amely már működőképes, de csak a legfontosabb funkciókat tartalmazza.

Később ezek fokozatosan tovább bővíthetők.

3. A szolgáltatások összekapcsolása

A mikroszolgáltatások kommunikálhatnak például:

REST API-val,
vagy gRPC segítségével.

A külső kérések kezelésére API Gateway használata ajánlott.

4. Service Discovery bevezetése

A szolgáltatások automatikus egymásra találásához használható:

Consul
vagy Eureka.
5. Monitorozás és naplózás

A rendszer állapotának figyelésére ajánlott:

Prometheus
Grafana

A naplók központi kezelésére:

ELK Stack.
6. Telepítés

Éles környezetben a mikroszolgáltatások futtatásához használható:

Docker Swarm
vagy Kubernetes.

Itt érdemes automatikus skálázást (Auto Scaling) és terheléselosztást (Load Balancing) is alkalmazni.

7. CI/CD kialakítása

A teljes fejlesztési folyamat automatizálható:

kód fordítása,
tesztelés,
Docker image-ek elkészítése,
telepítés.

Ehhez használható például:

Jenkins,
GitLab CI,
GitHub Actions.
