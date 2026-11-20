# CLAUDE.md — QuickJob Backend

Ce fichier guide Claude Code lorsqu'il travaille sur le backend de QuickJob. Il complète le `CLAUDE.md` racine du dépôt (vision produit, vocabulaire). Lis-le en entier avant toute modification.

## Le projet en bref

QuickJob est une place de marché biface de **micro-missions (1 à 2 heures)** en Afrique de l'Ouest. Un **donneur d'ordre** publie une mission géolocalisée (type de tâche, lieu, durée, rémunération) ; des **chercheurs de mission** à proximité la voient, postulent, et l'un d'eux est retenu. Le paiement passe par Mobile Money.

Villes de lancement : Lomé (pilote), puis Cotonou, Abidjan, Accra.

Le cœur du MVP est le **flux d'enregistrement d'une mission** et la **recherche de missions par proximité**. En cas de doute sur une priorité, privilégie ces deux flux.

## Stack technique

- Java 21, Spring Boot 3.x, Maven (wrapper `./mvnw`)
- PostgreSQL 16 + PostGIS 3.4
- Spring Data JPA + Hibernate Spatial (types JTS : `org.locationtech.jts.geom.Point`)
- Liquibase pour toutes les migrations de schéma (changelogs au format SQL)
- Spring Security + JWT (authentification par numéro de téléphone + OTP)
- Bean Validation (`jakarta.validation`)
- springdoc-openapi pour la documentation de l'API
- Tests : JUnit 5, AssertJ, Testcontainers (image `postgis/postgis`)

## Commandes

```bash
docker compose up -d db          # Démarre PostgreSQL/PostGIS en local
./mvnw spring-boot:run           # Lance l'application (profil dev)
./mvnw test                      # Tests unitaires
./mvnw verify                    # Tests unitaires + intégration (Testcontainers, Docker requis)
./mvnw liquibase:status          # Changesets non encore appliqués
./mvnw liquibase:rollbackSQL -Dliquibase.rollbackCount=1   # Aperçu SQL du rollback du dernier changeset
```

Lance `./mvnw verify` avant de considérer une tâche terminée.

## Architecture

Organisation **par fonctionnalité** (package-by-feature), pas par couche technique :

```
com.quickjob
├── mission/        # Enregistrement, recherche, cycle de vie des missions
├── application/    # Candidatures des chercheurs aux missions
├── user/           # Comptes, profils, rôles
├── auth/           # OTP, JWT
├── verification/   # Vérification d'identité (téléphone, pièce d'identité)
├── rating/         # Notes et avis mutuels
├── payment/        # Mobile Money, commissions
├── notification/   # Notifications temps réel (push)
└── common/         # Erreurs, config, utilitaires géo, sécurité partagée
```

Dans chaque package : `XxxController` → `XxxService` → `XxxRepository`, plus `dto/` et `XxxMapper`.

Mapping entités ↔ DTO **écrit à la main**, sans bibliothèque de mapping (pas de MapStruct, ModelMapper, etc.) :
- Les DTO sont des `record` Java.
- `XxxMapper` est un `@Component` avec des méthodes explicites (`toResponse(Mission)`, `toEntity(CreateMissionRequest, User)`), champ par champ.
- Le mapper ne contient aucune logique métier et n'appelle pas de repository. Les conversions géo passent par `GeoUtils`, les montants par `Money`.

Règles :
- Les controllers ne contiennent aucune logique métier ; ils valident, délèguent au service et mappent la réponse.
- Les entités JPA ne sortent jamais d'un controller : toujours un DTO.
- Les transactions (`@Transactional`) se placent sur les services, jamais sur les controllers.
- Un package n'accède pas directement au repository d'un autre package ; il passe par son service.

## Langue et nommage

Le code (classes, méthodes, colonnes, endpoints) est **en anglais**. Les commentaires, la documentation et les messages d'erreur destinés aux utilisateurs sont **en français**.

| Métier (FR)          | Code (EN)                 |
|----------------------|---------------------------|
| Mission              | `Mission`                 |
| Donneur d'ordre      | `Client` (rôle `CLIENT`)  |
| Chercheur de mission | `Worker` (rôle `WORKER`)  |
| Candidature          | `Application`             |
| Avis / note          | `Rating`                  |

Un même utilisateur peut avoir les deux rôles. Si le `CLAUDE.md` racine fixe d'autres identifiants, aligne-toi sur lui et signale l'écart.

## Domaine : la mission

### Enregistrement d'une mission (flux prioritaire)

`POST /api/v1/missions` — réservé au rôle `CLIENT`, utilisateur au téléphone vérifié.

Champs attendus :
- `category` : enum (`DELIVERY`, `HANDLING`, `CLEANING`, `BABYSITTING`, `GUARDING`, `IT_SUPPORT`, `ERRANDS`, `DIY`, `EVENT`, `OTHER`)
- `title` (max 100 car.), `description` (max 1000 car.)
- `latitude`, `longitude` + `addressLabel` (texte libre, ex. « Bè, près du marché »)
- `startAt` : date/heure de début souhaitée
- `durationMinutes`
- `remuneration` : montant + devise

Règles de validation :
- `durationMinutes` entre 60 et 120 par défaut (bornes configurables via `quickjob.mission.min-duration` / `max-duration`).
- `startAt` doit être dans le futur et dans une fenêtre configurable (`quickjob.mission.max-advance`, défaut 48 h). QuickJob vise l'immédiat.
- Coordonnées valides (lat ∈ [-90, 90], lon ∈ [-180, 180]) et dans une zone de service active (entité `ServiceZone`, polygone PostGIS par ville).
- Rémunération strictement positive et au-dessus d'un plancher configurable par devise.

À la création, la mission passe en `PUBLISHED`, puis le service publie un événement `MissionPublishedEvent` qui déclenche la notification des workers proches, de façon asynchrone et après commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`).

### Cycle de vie

```
PUBLISHED ──> ASSIGNED ──> IN_PROGRESS ──> COMPLETED
    │             │              │
    ├──> EXPIRED  └──> CANCELLED <┘
    └──> CANCELLED
```

- Les transitions passent **uniquement** par des méthodes de l'entité `Mission` (`assign()`, `start()`, `complete()`, `cancel()`, `expire()`) qui lèvent une exception si la transition est interdite. Ne modifie jamais le champ `status` directement.
- Une tâche planifiée passe en `EXPIRED` les missions `PUBLISHED` dont `startAt` est dépassé sans attribution.
- Verrouillage optimiste (`@Version`) sur `Mission` : une mission ne doit jamais être attribuée à deux workers.

## Géolocalisation (PostGIS)

- Stocke les positions en `geography(Point, 4326)` (distances directement en mètres).
- **Ordre des coordonnées : longitude d'abord** dans PostGIS et JTS (`new Coordinate(lon, lat)`). C'est la source de bugs la plus fréquente ; l'API expose toujours `latitude` / `longitude` nommés explicitement.
- Crée les points via une fabrique unique dans `common/geo` (`GeoUtils.point(lat, lon)`) avec SRID 4326.
- Toute colonne géographique a un index GiST, créé dans le même changeset Liquibase que la colonne.
- Recherche de proximité : filtre avec `ST_DWithin` (qui exploite l'index), trie par `ST_Distance`. N'utilise jamais `ST_Distance(...) < x` dans un `WHERE`.

Requête de référence :

```sql
SELECT m.*, ST_Distance(m.location, :point) AS distance_m
FROM missions m
WHERE m.status = 'PUBLISHED'
  AND ST_DWithin(m.location, :point, :radiusMeters)
ORDER BY distance_m
LIMIT :limit;
```

`GET /api/v1/missions/nearby?lat=&lon=&radius=` — rayon par défaut 5 km, maximum 20 km, pagination obligatoire.

Ne renvoie jamais la position exacte d'une mission à un worker non retenu : expose une distance arrondie et le `addressLabel`. Les coordonnées précises ne sont visibles qu'après attribution.

## Argent et paiements

- Montants stockés en **entier, unité mineure** (`BIGINT`) + code devise ISO 4217 : `XOF` pour Togo, Bénin et Côte d'Ivoire, `GHS` pour le Ghana. Le franc CFA n'a pas de subdivision (1 500 XOF = `1500`) ; le cedi se stocke en pesewas.
- Jamais de `double` ni de `float` pour un montant. En Java, utilise un value object `Money` (`long` + `Currency`).
- Commission plateforme : 8 à 12 %, configurable, calculée côté serveur uniquement, arrondie à l'unité mineure inférieure en faveur de l'utilisateur.
- Les opérateurs (Wave, Orange Money, Flooz, T-Money) sont derrière une interface `MobileMoneyProvider`, une implémentation par opérateur. Aucun code métier ne dépend d'un opérateur précis.
- Les webhooks des opérateurs sont **idempotents** (clé d'idempotence stockée en base) et leur signature est vérifiée avant tout traitement.
- En dev et en test, utilise `FakeMobileMoneyProvider`. N'appelle jamais une API de paiement réelle depuis un test.

## API

- Préfixe `/api/v1`, REST, JSON, `camelCase`.
- Erreurs au format RFC 7807 (`ProblemDetail`) via un `@RestControllerAdvice` unique dans `common/`. Message en français dans `detail`, code machine stable dans une propriété `code` (ex. `MISSION_INVALID_DURATION`).
- Dates en ISO 8601 avec décalage (`OffsetDateTime`), stockées en UTC (`timestamptz`). Lomé, Abidjan et Accra sont en UTC+0, Cotonou en UTC+1.
- Toute liste est paginée.
- Documente chaque nouvel endpoint avec les annotations OpenAPI.

## Base de données

### Migrations Liquibase

- Fichier maître : `src/main/resources/db/changelog/db.changelog-master.yaml`, déclaré via `spring.liquibase.change-log`. Il inclut les fichiers dans l'ordre avec `include` explicites (pas de `includeAll`, dont l'ordre dépend des noms de fichiers).
- Un fichier par évolution, au format **SQL formaté Liquibase**, dans `db/changelog/changes/` : `NNN-description.sql` (ex. `003-create-missions.sql`). Le SQL brut est préféré au YAML/XML car les types et fonctions PostGIS (`geography(Point, 4326)`, index GiST) s'y écrivent directement.
- Chaque fichier commence par `--liquibase formatted sql`. Chaque changeset est déclaré par `--changeset quickjob:NNN-description` et contient un seul changement logique.
- Chaque changeset a un bloc `--rollback` (ou `--rollback empty` justifié en commentaire si le retour arrière est impossible).
- Ne modifie **jamais** un changeset déjà appliqué : Liquibase vérifie sa somme de contrôle et l'application refusera de démarrer. Pour corriger, ajoute un nouveau changeset. N'utilise pas `runOnChange` ni `validCheckSum` pour contourner ce contrôle.
- Les données de référence (catégories, zones de service) vont dans des changesets dédiés ; les données de test utilisent `context:dev` ou `context:test`, jamais chargées en production.
- `spring.jpa.hibernate.ddl-auto=validate` dans tous les profils, jamais `update` ni `create` : le schéma vient uniquement de Liquibase.

Exemple :

```sql
--liquibase formatted sql

--changeset quickjob:003-create-missions
CREATE TABLE missions (
    id               UUID PRIMARY KEY,
    client_id        UUID NOT NULL REFERENCES users(id),
    category         VARCHAR(30) NOT NULL,
    title            VARCHAR(100) NOT NULL,
    description      VARCHAR(1000),
    location         geography(Point, 4326) NOT NULL,
    address_label    VARCHAR(255) NOT NULL,
    start_at         TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL,
    amount_minor     BIGINT NOT NULL CHECK (amount_minor > 0),
    currency         CHAR(3) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_missions_location ON missions USING GIST (location);
CREATE INDEX idx_missions_status_start_at ON missions (status, start_at);
--rollback DROP TABLE missions;
```

### Conventions de schéma

- Identifiants en `UUID`.
- Tables et colonnes en `snake_case`, tables au pluriel (`missions`, `applications`).
- Colonnes `created_at` et `updated_at` sur toutes les tables.
- Le premier changeset (`001-enable-postgis.sql`) active PostGIS : `CREATE EXTENSION IF NOT EXISTS postgis;`.

## Sécurité et données personnelles

- Authentification : OTP par SMS sur le numéro de téléphone (format E.164, ex. `+22890000000`), puis JWT d'accès court + refresh token.
- Les pièces d'identité sont stockées hors base (stockage objet), chiffrées, accessibles uniquement par URL signée à durée limitée.
- Ne logue jamais : numéros de téléphone complets, codes OTP, tokens, coordonnées GPS précises, données de pièce d'identité. Masque-les (`+228****0000`).
- Vérifie l'autorisation au niveau de l'objet : un client ne modifie que ses missions, un worker ne voit que ses candidatures.
- Limite le débit des endpoints OTP et d'enregistrement de mission.
- Aucun secret dans le code ni dans `application.yml` : variables d'environnement uniquement.

## Tests

- Tests unitaires pour la logique métier (transitions d'état, calcul de commission, validation).
- Tests d'intégration avec Testcontainers et une vraie base PostGIS pour toute requête spatiale. Ne mocke pas PostGIS et n'utilise pas H2.
- Pour chaque endpoint : au moins un cas nominal, un cas de validation en échec et un cas d'accès non autorisé.
- Données de test centrées sur Lomé (ex. Grand Marché : lat 6.1319, lon 1.2228).

## À ne pas faire

- Inverser latitude et longitude.
- Exposer une entité JPA dans une réponse HTTP.
- Changer le statut d'une mission sans passer par les méthodes de l'entité.
- Utiliser des nombres à virgule flottante pour l'argent.
- Ajouter une dépendance Maven sans le signaler et le justifier.
- Modifier ou supprimer un changeset Liquibase déjà commité.
- Ajouter une bibliothèque de mapping (MapStruct, ModelMapper…).

## Décisions encore ouvertes

Si une tâche touche l'un de ces points, demande avant de trancher :
- Moment du paiement (séquestre à l'attribution ou paiement en fin de mission).
- Politique d'annulation et pénalités éventuelles.
- Fournisseur de notifications push (FCM ou autre) et de SMS pour l'OTP.
- Modèle de l'abonnement Premium et de la publicité locale (hors MVP).
