# Alkalmazás futtatása másik gépen Dockerrel (VirtualBox VM)

Ez az útmutató azt írja le, hogyan futtatod a Scalable E-Commerce Platformot egy
másik gépen (VirtualBox VM) Dockerrel, a GitHub Container Registry-ből (GHCR)
lehúzott image-ekből — **forráskód nélkül, build nélkül**.

## Előfeltételek

1. **Linuxos VirtualBox VM** (pl. Ubuntu).
2. A VM-en telepített **Docker Engine** + **Docker Compose plugin** (compose v2):

   ```bash
   sudo apt update
   sudo apt install -y docker.io docker-compose-v2
   sudo systemctl enable --now docker
   ```

3. A 9 szolgáltatás image-nek **léteznie kell a GHCR-ben** (a CI `docker-publish`
   job-ja hozza létre őket), és **privát** beállításként kell elérhetőnek lenniük.

## 1. A projekt leklónozása

A `docker-compose.yml` és a `scripts/seed.sql` (adatbázis inicializálás) a
repositoryban van, ezért klónozd:

```bash
git clone https://github.com/kenyereskrisztian/Scalable_E-Commerce_Platform.git
cd Scalable_E-Commerce_Platform
```

## 2. Bejelentkezés a GHCR-be (privát image-ekhez)

A privát image-ek lehúzásához a Docker-nek hitelesítés kell. Ehhez egy
**Personal Access Token (PAT)** szükséges, amit a GitHubon hozol létre:

- GitHub → Settings → Developer settings → Personal access tokens →
  **Fine-grained tokens**
- Jogosultság: a repository-ra `Packages → Read`
- (Vagy klasszikus token `read:packages` scop-pal)

A VM-ben, a klónozott mappában:

```bash
echo <A_TOKEN_BEIRASA> | docker login ghcr.io -u <github-felhasznalonev> --password-stdin
```

> **Fontos:** A tokent SOHA ne commitold a repositoryba! A `docker login` a
> `~/.docker/config.json` fájlban tárolja. A token a lejáratáig érvényes,
> ennél a célnál elegendő a `read:packages` jog.

## 3. Image-ek letöltése és a stack indítása

```bash
docker compose pull      # a 9 image + a kiegészítő szolgáltatások lehúzása
docker compose up -d     # a stack elindítása háttérben
docker compose ps        # státuszok ellenőrzése (healthy / running)
```

A `docker compose.yml` a 9 szolgáltatást `ghcr.io/kenyereskrisztian/<service>:latest`
image-ként hivatkozza, így a pull azokat tölti le.

## 4. Adatbázis

Az első indításkor a `db` (MySQL) szolgáltatás automatikusan lefuttatja a
`scripts/seed.sql`-t, ami létrehozza a négy adatbázist
(`ecommerce_users`, `ecommerce_products`, `ecommerce_cart`, `ecommerce_orders`)
és feltölti a tesztadatokat (demo felhasználók, termékek, kategóriák).

- A szolgáltatások a `ddl-auto: update` beállítással tükrözik a táblákat.
- **Nem szükséges** a régi gép adatbázis-volume-jának átvitele, ha a tesztadatok elegendőek.

## 5. Elérés a VM böngészőjéből

A VM saját böngészőjében a következő címeken érhetők el a szolgáltatások:

| Szolgáltatás | Cím |
|---|---|
| Frontend | `http://localhost:5500` |
| API Gateway | `http://localhost:8080` |
| Eureka (discovery) | `http://localhost:8761` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Kibana | `http://localhost:5601` |

## 6. A GHCR image-ek privátra állítása

Ha még nem privát, állítsd át a GitHub Package oldalán:

- Repository → Packages → az image → Settings → Visibility → **Private**

Ezután az image-ek lehúzása **kizárólag** a bejelentkezett, `read:packages`
jogosultságú felhasználótól lehetséges.

## Hibakeresés

```bash
docker compose ps                 # szolgáltatások státusza
docker compose logs <service>     # egy szolgáltatás naplói
docker compose down               # leállítás (a volume adatai megmaradnak)
docker compose down -v            # leállítás + a volume-ok törlése (friss DB)
```

Ha egy szolgáltatás nem indul, az általában a `db` (healthcheck) vagy a
`discovery-service` függőségére vár — a `docker compose ps` mutatja a HEALTH
oszlopot.
