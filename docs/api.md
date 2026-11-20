# QuickJob API — Guide d'integration Flutter

Ce document liste tous les endpoints du backend QuickJob dans l'ordre d'integration recommande.
Le developpeur Flutter doit les implementer de haut en bas : chaque section depend des precedentes.

---

## Conventions generales

| Element | Valeur |
|---------|--------|
| Base URL | `/api/v1` |
| Format | JSON, `camelCase` |
| Dates | ISO 8601 avec offset : `2026-11-18T14:00:00+00:00` |
| Pagination | `?page=0&size=20` (defaut 20, max 50) |
| Auth | Header `Authorization: Bearer <accessToken>` |
| Telephone | Format E.164 : `+22890123456` |
| Montants | Entier en unite mineure (`1500` = 1 500 XOF, `1550` = 15.50 GHS) |
| Devises | `XOF` (Togo, Benin, Cote d'Ivoire), `GHS` (Ghana) |

### Format d'erreur (RFC 7807)

Toutes les erreurs suivent ce format :

```json
{
  "type": "https://quickjob.app/errors/MISSION_NOT_FOUND",
  "title": "Not Found",
  "status": 404,
  "detail": "Mission introuvable",
  "code": "MISSION_NOT_FOUND"
}
```

Le champ `code` est une chaine stable exploitable cote client pour la logique conditionnelle.
Le champ `detail` est un message dans la langue de l'utilisateur, affichable directement.

### Internationalisation (i18n)

Les messages d'erreur et de succes sont traduits en **francais** (defaut) et **anglais**.

**Resolution de la langue** :

1. Si l'utilisateur est authentifie : sa `preferredLanguage` en base
2. Sinon : header HTTP `Accept-Language`
3. Fallback : `fr`

**Pour le client Flutter** :
- Envoyer `Accept-Language: en` ou `Accept-Language: fr` dans chaque requete HTTP
- Apres inscription/connexion, appeler `PUT /users/me/language` pour persister la preference
- Les notifications push utilisent la langue stockee en base (pas le header HTTP)

### Pagination

Les reponses paginees suivent le format Spring :

```json
{
  "content": [ ... ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

---

## 1. Authentification

Endpoints publics, aucun token requis.

### 1.1 Demander un OTP

```
POST /api/v1/auth/otp/request
```

**Rate limit** : 5 req/min par IP.

**Body** :

```json
{
  "phoneNumber": "+22890123456"
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `phoneNumber` | string | oui | E.164 : `^\+\d{10,15}$` |

**Reponse** `200` :

```json
{
  "message": "Code OTP envoyé"
}
```

---

### 1.2 Creer un compte

```
POST /api/v1/auth/register
```

**Rate limit** : 10 req/min par IP.

**Body** :

```json
{
  "phoneNumber": "+22890123456",
  "displayName": "Jean Kodjo",
  "otpCode": "123456",
  "preferredLanguage": "fr"
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `phoneNumber` | string | oui | E.164 |
| `displayName` | string | oui | 2-100 caracteres |
| `otpCode` | string | oui | 6 chiffres : `^\d{6}$` |
| `preferredLanguage` | string | non | `fr` ou `en` (defaut : `fr`) |

**Reponse** `201` :

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "a1b2c3d4-...",
  "expiresIn": 900
}
```

| Champ | Description |
|-------|-------------|
| `accessToken` | JWT a placer dans le header `Authorization: Bearer ...` |
| `refreshToken` | Token de rafraichissement (a stocker de facon securisee) |
| `expiresIn` | Duree de vie du access token en secondes (900 = 15 min) |

**Erreurs possibles** :

| Code | HTTP | Cause |
|------|------|-------|
| `AUTH_PHONE_ALREADY_REGISTERED` | 409 | Numero deja inscrit |
| `AUTH_INVALID_OTP` | 400 | Code OTP incorrect ou expire |

---

### 1.3 Se connecter

```
POST /api/v1/auth/login
```

**Rate limit** : 10 req/min par IP.

**Body** :

```json
{
  "phoneNumber": "+22890123456",
  "otpCode": "123456"
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `phoneNumber` | string | oui | E.164 |
| `otpCode` | string | oui | 6 chiffres |

**Reponse** `200` : meme format que `register`.

**Erreurs possibles** :

| Code | HTTP | Cause |
|------|------|-------|
| `AUTH_USER_NOT_REGISTERED` | 404 | Aucun compte avec ce numero |
| `AUTH_INVALID_OTP` | 400 | Code OTP incorrect ou expire |

---

### 1.4 Rafraichir le token

```
POST /api/v1/auth/refresh
```

**Body** :

```json
{
  "refreshToken": "a1b2c3d4-..."
}
```

**Reponse** `200` : meme format que `register` (nouveaux tokens).

**Erreur** : `AUTH_INVALID_TOKEN` (401) si le refresh token est invalide ou revoque.

> **Important** : implementer le rafraichissement automatique dans un interceptor HTTP.
> Rafraichir le token **avant** son expiration, ou lors d'un 401.

---

### 1.5 Se deconnecter

```
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

**Reponse** `200` :

```json
{
  "message": "Déconnexion réussie"
}
```

Revoque tous les refresh tokens de l'utilisateur.

---

## 2. Profil utilisateur

Tous les endpoints ci-dessous necessitent un token valide.

### 2.1 Mon profil

```
GET /api/v1/users/me
```

**Reponse** `200` :

```json
{
  "id": "uuid",
  "phoneNumber": "+22890123456",
  "displayName": "Jean Kodjo",
  "roles": ["CLIENT", "WORKER"],
  "workerCategories": ["DELIVERY", "HANDLING"],
  "phoneVerified": true,
  "preferredLanguage": "fr",
  "clientRating": {
    "averageScore": 4.5,
    "totalRatings": 12
  },
  "workerRating": {
    "averageScore": 4.8,
    "totalRatings": 25
  },
  "createdAt": "2026-01-15T10:30:00Z"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `id` | UUID | Identifiant unique |
| `phoneNumber` | string | Numero E.164 |
| `displayName` | string | Nom affiche |
| `roles` | string[] | `CLIENT`, `WORKER` ou les deux |
| `workerCategories` | string[] | Categories pour lesquelles le worker est disponible (vide = polyvalent) |
| `phoneVerified` | boolean | Telephone verifie |
| `preferredLanguage` | string | Langue preferee (`fr` ou `en`) |
| `clientRating` | RatingStats | Moyenne et nombre d'avis recus en tant que client |
| `workerRating` | RatingStats | Moyenne et nombre d'avis recus en tant que worker |
| `createdAt` | datetime | Date d'inscription |

**RatingStats** :

| Champ | Type | Description |
|-------|------|-------------|
| `averageScore` | double | Moyenne arrondie a 1 decimale (0.0 si aucun avis) |
| `totalRatings` | long | Nombre total d'avis |

---

### 2.2 Mettre a jour ma position

```
PUT /api/v1/users/me/location
```

**Body** :

```json
{
  "latitude": 6.1319,
  "longitude": 1.2228
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `latitude` | double | oui | -90 a 90 |
| `longitude` | double | oui | -180 a 180 |

**Reponse** : `204 No Content`

> **Usage** : le worker met a jour sa position manuellement via un bouton dans l'application.
> La position ne perime pas : elle reste valide jusqu'a la prochaine mise a jour.

---

### 2.3 Mettre a jour mes categories

```
PUT /api/v1/users/me/categories
```

**Body** :

```json
{
  "categories": ["DELIVERY", "HANDLING", "CLEANING"]
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `categories` | string[] | oui | Valeurs de `MissionCategory` (voir section Enums) |

**Reponse** : `204 No Content`

> Un ensemble vide `[]` signifie que le worker est polyvalent et recevra les notifications
> pour toutes les categories.

---

### 2.4 Changer ma langue preferee

```
PUT /api/v1/users/me/language
```

**Body** :

```json
{
  "language": "en"
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `language` | string | oui | `fr` ou `en` |

**Reponse** : `204 No Content`

> Affecte la langue des messages d'erreur (requetes authentifiees) et des notifications push.

---

## 3. Missions

### 3.0 Liste des categories

```
GET /api/v1/missions/categories
```

**Authentification** : aucune (endpoint public).

**Reponse** `200` :

```json
["DELIVERY","HANDLING","CLEANING","BABYSITTING","GUARDING","IT_SUPPORT","ERRANDS","DIY","EVENT","OTHER"]
```

> Utiliser cet endpoint pour alimenter dynamiquement les listes deroulantes cote Flutter
> plutot que de coder les valeurs en dur.

---

### Enums de reference

**MissionCategory** :

| Valeur | Description |
|--------|-------------|
| `DELIVERY` | Livraison |
| `HANDLING` | Manutention |
| `CLEANING` | Nettoyage |
| `BABYSITTING` | Garde d'enfants |
| `GUARDING` | Gardiennage |
| `IT_SUPPORT` | Support informatique |
| `ERRANDS` | Courses |
| `DIY` | Bricolage |
| `EVENT` | Evenementiel |
| `OTHER` | Autre |

**MissionStatus** :

| Valeur | Description |
|--------|-------------|
| `PUBLISHED` | Publiee, en attente de candidatures |
| `ASSIGNED` | Worker(s) attribue(s), pas encore demarree |
| `IN_PROGRESS` | En cours |
| `COMPLETED` | Terminee |
| `CANCELLED` | Annulee par le client |
| `EXPIRED` | Date de debut depassee sans attribution |

**Transitions autorisees** :

```
PUBLISHED --> ASSIGNED --> IN_PROGRESS --> COMPLETED
    |             |              |
    +-> EXPIRED   +-> CANCELLED  +-> CANCELLED
    +-> CANCELLED
```

---

### 3.1 Creer une mission

```
POST /api/v1/missions
```

**Role requis** : `CLIENT` avec telephone verifie.

**Body** :

```json
{
  "category": "DELIVERY",
  "title": "Livraison de colis urgent",
  "description": "Livrer un colis de 5 kg au marche central",
  "latitude": 6.1319,
  "longitude": 1.2228,
  "addressLabel": "Be, pres du marche central",
  "startAt": "2026-11-18T14:00:00+00:00",
  "durationMinutes": 90,
  "amountMinor": 2000,
  "currency": "XOF",
  "requiredWorkers": 1,
  "notificationRadiusMeters": 5000,
  "targetWorkerCategories": ["DELIVERY"]
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `category` | string | oui | Valeur de `MissionCategory` |
| `title` | string | oui | Max 100 caracteres |
| `description` | string | non | Max 1000 caracteres |
| `latitude` | double | oui | -90 a 90, dans une zone de service active |
| `longitude` | double | oui | -180 a 180, dans une zone de service active |
| `addressLabel` | string | oui | Max 255 caracteres (ex: "Be, pres du marche") |
| `startAt` | datetime | oui | Dans le futur, max 48h a l'avance |
| `durationMinutes` | int | oui | 60-120 minutes |
| `amountMinor` | long | oui | Positif, min 500 XOF / 5 GHS |
| `currency` | string | oui | `XOF` ou `GHS` (3 lettres ISO 4217) |
| `requiredWorkers` | int | non | 1-5 (defaut : 1) |
| `notificationRadiusMeters` | int | non | Rayon de notification en metres (defaut : 5 km, max : 20 km) |
| `targetWorkerCategories` | string[] | non | Categories de workers a cibler pour la notification |
| `deliveryLatitude` | double | non | Latitude du lieu de livraison (-90 a 90, dans une zone de service) |
| `deliveryLongitude` | double | non | Longitude du lieu de livraison (-180 a 180, dans une zone de service) |
| `deliveryAddressLabel` | string | non | Adresse textuelle du lieu de livraison (max 255 car.) |
| `additionalInfo` | string | non | Informations complementaires (max 2000 car.) |

**Reponse** `201` : `MissionResponse` (voir ci-dessous).

**Erreurs possibles** :

| Code | HTTP | Cause |
|------|------|-------|
| `USER_PHONE_NOT_VERIFIED` | 403 | Telephone non verifie |
| `MISSION_INVALID_DURATION` | 400 | Duree hors bornes |
| `MISSION_INVALID_START_TIME` | 400 | Date dans le passe ou trop lointaine |
| `MISSION_OUTSIDE_SERVICE_ZONE` | 400 | Coordonnees hors zone de service |
| `MISSION_INVALID_REMUNERATION` | 400 | Montant sous le plancher |
| `MISSION_INVALID_REQUIRED_WORKERS` | 400 | Nombre de workers hors bornes |
| `MISSION_INVALID_NOTIFICATION_RADIUS` | 400 | Rayon trop grand |
| `MISSION_INCOMPLETE_DELIVERY_COORDS` | 400 | Latitude ou longitude de livraison fournie sans l'autre |

---

### MissionResponse

Format retourne par tous les endpoints de mission (creation, detail, lifecycle, listes) :

```json
{
  "id": "uuid",
  "clientId": "uuid",
  "category": "DELIVERY",
  "title": "Livraison de colis urgent",
  "description": "Livrer un colis de 5 kg au marche central",
  "latitude": 6.1319,
  "longitude": 1.2228,
  "addressLabel": "Be, pres du marche central",
  "startAt": "2026-11-18T14:00:00+00:00",
  "durationMinutes": 90,
  "amountMinor": 2000,
  "currency": "XOF",
  "status": "PUBLISHED",
  "requiredWorkers": 1,
  "assignedWorkersCount": 0,
  "createdAt": "2026-11-18T10:30:00Z",
  "myRatingSubmitted": null,
  "deliveryLatitude": 6.1725,
  "deliveryLongitude": 1.2314,
  "deliveryAddressLabel": "Marche Adawlato, Lome",
  "additionalInfo": "Colis fragile, manipuler avec soin"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `latitude` | Double? | `null` si l'utilisateur n'est ni le client ni un worker attribue |
| `longitude` | Double? | Idem |
| `assignedWorkersCount` | int | Nombre de workers deja attribues |
| `myRatingSubmitted` | Boolean? | `null` si mission pas `COMPLETED`. `true`/`false` si completee : indique si l'utilisateur connecte a deja soumis sa note |
| `deliveryLatitude` | Double? | Latitude du lieu de livraison (`null` si non renseigne ou si vie privee) |
| `deliveryLongitude` | Double? | Longitude du lieu de livraison |
| `deliveryAddressLabel` | String? | Adresse textuelle du lieu de livraison |
| `additionalInfo` | String? | Informations complementaires selon la categorie |

> **Vie privee** : les coordonnees exactes (`latitude`/`longitude` et `deliveryLatitude`/`deliveryLongitude`)
> ne sont visibles que par le client proprietaire et les workers attribues. Les autres utilisateurs voient `null`.

---

### 3.2 Detail d'une mission

```
GET /api/v1/missions/{id}
```

**Reponse** `200` : `MissionResponse`.

---

### 3.3 Mes missions en tant que client

```
GET /api/v1/missions/me/client?page=0&size=20
```

**Reponse** `200` : page de `MissionResponse`.

> Utiliser `myRatingSubmitted` pour afficher un badge "A noter" sur les missions `COMPLETED`
> ou `myRatingSubmitted == false` et dont `completedAt` est dans les 7 derniers jours.

---

### 3.4 Mes missions en tant que worker

```
GET /api/v1/missions/me/worker?page=0&size=20
```

**Reponse** `200` : page de `MissionResponse`.

---

### 3.5 Demarrer une mission

```
POST /api/v1/missions/{id}/start
```

**Acces** : client proprietaire ou worker attribue.

**Transition** : `ASSIGNED` -> `IN_PROGRESS`.

**Reponse** `200` : `MissionResponse`.

**Erreur** : `MISSION_INVALID_TRANSITION` (409) si la mission n'est pas en `ASSIGNED`.

---

### 3.6 Terminer une mission

```
POST /api/v1/missions/{id}/complete
```

**Acces** : client proprietaire ou worker attribue.

**Transition** : `IN_PROGRESS` -> `COMPLETED`.

**Reponse** `200` : `MissionResponse`.

> A la completion, une notification `MISSION_COMPLETED` est envoyee automatiquement au client
> et a tous les workers attribues pour les inviter a noter.

---

### 3.7 Annuler une mission

```
POST /api/v1/missions/{id}/cancel
```

**Acces** : client proprietaire uniquement.

**Transitions possibles** : `PUBLISHED`, `ASSIGNED` ou `IN_PROGRESS` -> `CANCELLED`.

**Reponse** `200` : `MissionResponse`.

---

### 3.8 Recherche a proximite

```
GET /api/v1/missions/nearby?lat=6.1319&lon=1.2228&radius=5000&page=0&size=20
```

| Parametre | Type | Requis | Description |
|-----------|------|--------|-------------|
| `lat` | double | oui | Latitude du worker |
| `lon` | double | oui | Longitude du worker |
| `radius` | double | non | Rayon en metres (defaut : 5 000, max : 20 000) |
| `page`, `size` | int | non | Pagination |

**Reponse** `200` : page de `NearbyMissionResponse`.

---

### NearbyMissionResponse

Format retourne par les endpoints `/nearby` et `/feed` :

```json
{
  "id": "uuid",
  "category": "DELIVERY",
  "title": "Livraison de colis urgent",
  "description": "Livrer un colis de 5 kg au marche central",
  "addressLabel": "Be, pres du marche central",
  "distanceMeters": 1200,
  "approximateLatitude": 6.1325,
  "approximateLongitude": 1.2215,
  "startAt": "2026-11-18T14:00:00+00:00",
  "durationMinutes": 90,
  "amountMinor": 2000,
  "currency": "XOF",
  "requiredWorkers": 1,
  "assignedWorkersCount": 0,
  "createdAt": "2026-11-18T10:30:00Z",
  "deliveryAddressLabel": "Marche Adawlato, Lome",
  "additionalInfo": "Colis fragile, manipuler avec soin"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `distanceMeters` | long | Distance arrondie a 100 m |
| `approximateLatitude` | Double | Coordonnee bruitee +-200 m (pour l'onglet Maps) |
| `approximateLongitude` | Double | Coordonnee bruitee +-200 m |
| `deliveryAddressLabel` | String? | Adresse textuelle du lieu de livraison |
| `additionalInfo` | String? | Informations complementaires |

> **Maps** : utiliser `approximateLatitude` / `approximateLongitude` pour placer les pins
> sur la carte. Ces coordonnees sont deterministes (la meme mission donne toujours le meme
> point approximatif) mais decalees d'environ 200 m par rapport a la position reelle.

---

### 3.9 Flux Explorer (feed)

```
GET /api/v1/missions/feed?lat=6.1319&lon=1.2228&page=0&size=20
```

| Parametre | Type | Requis | Description |
|-----------|------|--------|-------------|
| `lat` | double | non | Pour le calcul de distance (0 si absent) |
| `lon` | double | non | Pour le calcul de distance (0 si absent) |
| `page`, `size` | int | non | Pagination |

**Reponse** `200` : page de `NearbyMissionResponse`.

**Comportement** :
- Retourne les missions `PUBLISHED` triees par date de creation (plus recentes d'abord)
- Filtrees par les categories preferees du worker (definies via `PUT /users/me/categories`)
- Si le worker est polyvalent (categories vides), toutes les missions sont affichees
- Exclut les missions auxquelles le worker a deja postule

> C'est l'endpoint principal pour la page "Explorer" de l'application.

---

## 4. Candidatures

**ApplicationStatus** :

| Valeur | Description |
|--------|-------------|
| `PENDING` | En attente de decision du client |
| `ACCEPTED` | Acceptee, worker attribue a la mission |
| `REJECTED` | Rejetee par le client |
| `CANCELLED` | Annulee par le worker |

---

### 4.1 Postuler a une mission

```
POST /api/v1/applications
```

**Rate limit** : 20 req/min par IP.

**Body** :

```json
{
  "missionId": "uuid",
  "message": "Je suis disponible et motive pour cette mission"
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `missionId` | UUID | oui | Mission existante en `PUBLISHED` |
| `message` | string | non | Max 500 caracteres |

**Reponse** `201` : `ApplicationResponse` (voir ci-dessous).

**Erreurs possibles** :

| Code | HTTP | Cause |
|------|------|-------|
| `APPLICATION_MISSION_NOT_PUBLISHED` | 409 | Mission pas en `PUBLISHED` |
| `APPLICATION_OWN_MISSION` | 409 | On ne peut pas postuler a sa propre mission |
| `APPLICATION_ALREADY_EXISTS` | 409 | Candidature deja soumise |
| `MISSION_FULLY_ASSIGNED` | 409 | Nombre de workers requis deja atteint |

---

### ApplicationResponse

```json
{
  "id": "uuid",
  "missionId": "uuid",
  "workerId": "uuid",
  "status": "PENDING",
  "message": "Je suis disponible et motive pour cette mission",
  "createdAt": "2026-11-18T10:30:00Z"
}
```

---

### 4.2 Accepter une candidature

```
POST /api/v1/applications/{id}/accept
```

**Acces** : client proprietaire de la mission uniquement.

**Reponse** `200` : `ApplicationResponse` avec `status: "ACCEPTED"`.

**Effets** :
- Attribue le worker a la mission
- Si le nombre de `requiredWorkers` est atteint, la mission passe en `ASSIGNED` et toutes les autres candidatures `PENDING` sont automatiquement rejetees
- Notification `APPLICATION_ACCEPTED` envoyee au worker

**Erreurs** :

| Code | HTTP | Cause |
|------|------|-------|
| `MISSION_FULLY_ASSIGNED` | 409 | Tous les postes sont deja pourvus |
| `APPLICATION_ACCESS_DENIED` | 403 | Pas proprietaire de la mission |

---

### 4.3 Rejeter une candidature

```
POST /api/v1/applications/{id}/reject
```

**Acces** : client proprietaire de la mission.

**Reponse** `200` : `ApplicationResponse` avec `status: "REJECTED"`.

Une notification `APPLICATION_REJECTED` est envoyee au worker.

---

### 4.4 Annuler sa candidature

```
POST /api/v1/applications/{id}/cancel
```

**Acces** : worker proprietaire de la candidature.

**Reponse** `200` : `ApplicationResponse` avec `status: "CANCELLED"`.

---

### 4.5 Candidatures d'une mission

```
GET /api/v1/applications/mission/{missionId}?page=0&size=20
```

**Acces** : client proprietaire de la mission.

**Reponse** `200` : page de `ApplicationResponse`.

---

### 4.6 Mes candidatures

```
GET /api/v1/applications/me?page=0&size=20
```

**Reponse** `200` : page de `ApplicationResponse`.

---

## 5. Notifications

### 5.1 Lister mes notifications

```
GET /api/v1/notifications?unreadOnly=false&page=0&size=20
```

| Parametre | Type | Requis | Description |
|-----------|------|--------|-------------|
| `unreadOnly` | boolean | non | `true` = non lues uniquement (defaut : `false`) |

**Reponse** `200` : page de `NotificationResponse`.

---

### NotificationResponse

```json
{
  "id": "uuid",
  "type": "NEW_MISSION",
  "payload": { ... },
  "read": false,
  "createdAt": "2026-11-18T10:30:00Z"
}
```

Le contenu de `payload` depend du `type` :

#### `NEW_MISSION` — Nouvelle mission a proximite

Destinataire : workers proches correspondant aux criteres.

```json
{
  "missionId": "uuid",
  "title": "Livraison de colis urgent",
  "category": "DELIVERY",
  "addressLabel": "Be, pres du marche central",
  "amountMinor": 2000,
  "currency": "XOF",
  "distanceMeters": 1200,
  "requiredWorkers": 1
}
```

#### `APPLICATION_ACCEPTED` — Candidature acceptee

Destinataire : worker dont la candidature a ete acceptee.

```json
{
  "applicationId": "uuid",
  "missionId": "uuid",
  "missionTitle": "Livraison de colis urgent",
  "status": "ACCEPTED"
}
```

#### `APPLICATION_REJECTED` — Candidature rejetee

Destinataire : worker dont la candidature a ete rejetee.

```json
{
  "applicationId": "uuid",
  "missionId": "uuid",
  "missionTitle": "Livraison de colis urgent",
  "status": "REJECTED"
}
```

#### `MISSION_COMPLETED` — Mission terminee, invitation a noter

Destinataire : client ET tous les workers attribues.

```json
{
  "missionId": "uuid",
  "message": "Notez votre experience pour cette mission"
}
```

> A la reception de cette notification, afficher un popup/bottom sheet invitant l'utilisateur
> a noter la mission. Le client note le(s) worker(s), le worker note le client.

---

### 5.2 Marquer une notification comme lue

```
PATCH /api/v1/notifications/{id}/read
```

**Reponse** : `204 No Content`.

---

### 5.3 Tout marquer comme lu

```
POST /api/v1/notifications/read-all
```

**Reponse** : `204 No Content`.

---

### 5.4 WebSocket (temps reel)

Les notifications sont egalement poussees en temps reel via WebSocket STOMP/SockJS.

| Element | Valeur |
|---------|--------|
| Endpoint | `/ws` |
| Protocole | STOMP sur SockJS |
| Auth | Query param `?token=<accessToken>` lors du handshake, ou header STOMP `Authorization` lors du CONNECT |
| Canal | S'abonner a `/user/queue/notifications` |

**Flux** :

1. Se connecter a `ws://<host>/ws?token=<accessToken>`
2. Envoyer une frame STOMP CONNECT
3. S'abonner a `/user/queue/notifications`
4. Recevoir les notifications en temps reel (meme format que `NotificationResponse`)

> Les notifications sont **toujours** persistees en base. Le WebSocket est un bonus
> pour la reception instantanee. Si la connexion WebSocket est absente, les notifications
> sont recuperees via `GET /notifications` a la prochaine ouverture de l'app.

---

## 6. Notation (Ratings)

Apres la completion d'une mission, le client et les workers disposent d'une fenetre de **7 jours**
pour se noter mutuellement.

**RatedRole** :

| Valeur | Signification |
|--------|---------------|
| `CLIENT` | On note le client (c'est un worker qui soumet l'avis) |
| `WORKER` | On note le worker (c'est le client qui soumet l'avis) |

---

### 6.1 Soumettre une note

```
POST /api/v1/missions/{missionId}/ratings
```

**Body** :

```json
{
  "roleRated": "WORKER",
  "score": 5,
  "comment": "Excellent travail, rapide et soigne",
  "ratedUserId": "uuid"
}
```

| Champ | Type | Requis | Validation |
|-------|------|--------|------------|
| `roleRated` | string | oui | `CLIENT` ou `WORKER` |
| `score` | int | oui | 1 a 5 |
| `comment` | string | non | Max 500 caracteres |
| `ratedUserId` | UUID | conditionnel | Requis si `roleRated == WORKER` et mission multi-workers. Ignore si mission mono-worker (infere automatiquement) |

**Reponse** `201` :

```json
{
  "id": "uuid",
  "missionId": "uuid",
  "raterId": "uuid",
  "ratedId": "uuid",
  "roleRated": "WORKER",
  "score": 5,
  "comment": "Excellent travail, rapide et soigne",
  "createdAt": "2026-11-18T10:30:00Z"
}
```

**Erreurs possibles** :

| Code | HTTP | Cause |
|------|------|-------|
| `RATING_MISSION_NOT_COMPLETED` | 400 | Mission pas en `COMPLETED` |
| `RATING_WINDOW_EXPIRED` | 400 | Plus de 7 jours depuis la completion |
| `RATING_ALREADY_EXISTS` | 409 | Note deja soumise pour cette mission |
| `RATING_UNAUTHORIZED` | 403 | Pas autorise a noter (ni client ni worker attribue) |
| `RATING_SELF_RATING` | 400 | On ne peut pas se noter soi-meme |

**Regles metier** :

- Un **worker** ne peut soumettre qu'un avis avec `roleRated: "CLIENT"` (il note le client)
- Un **client** ne peut soumettre qu'un avis avec `roleRated: "WORKER"` (il note le worker)
- Chaque utilisateur ne peut noter qu'une seule fois par mission
- Les moyennes sont recalculees et visibles dans `GET /users/me` via `clientRating` et `workerRating`

---

## 7. Codes d'erreur complets

| Code | HTTP | Description |
|------|------|-------------|
| **Auth** | | |
| `AUTH_INVALID_TOKEN` | 401 | Token invalide ou expire |
| `AUTH_INVALID_OTP` | 400 | Code OTP incorrect |
| `AUTH_USER_NOT_REGISTERED` | 404 | Utilisateur non inscrit |
| `AUTH_PHONE_ALREADY_REGISTERED` | 409 | Numero deja enregistre |
| `AUTH_ACCESS_DENIED` | 403 | Acces refuse |
| **User** | | |
| `USER_NOT_FOUND` | 404 | Utilisateur introuvable |
| `USER_PHONE_NOT_VERIFIED` | 403 | Telephone non verifie |
| **Mission** | | |
| `MISSION_NOT_FOUND` | 404 | Mission introuvable |
| `MISSION_INVALID_DURATION` | 400 | Duree hors 60-120 min |
| `MISSION_INVALID_START_TIME` | 400 | Date invalide |
| `MISSION_OUTSIDE_SERVICE_ZONE` | 400 | Hors zone de service |
| `MISSION_INVALID_REMUNERATION` | 400 | Montant trop bas |
| `MISSION_INVALID_TRANSITION` | 409 | Transition de statut interdite |
| `MISSION_INVALID_REQUIRED_WORKERS` | 400 | Nombre de workers invalide |
| `MISSION_WORKER_ALREADY_ASSIGNED` | 409 | Worker deja attribue |
| `MISSION_FULLY_ASSIGNED` | 409 | Tous les postes pourvus |
| `MISSION_INVALID_NOTIFICATION_RADIUS` | 400 | Rayon de notification invalide |
| `MISSION_INCOMPLETE_DELIVERY_COORDS` | 400 | Coordonnees de livraison incompletes |
| **Application** | | |
| `APPLICATION_NOT_FOUND` | 404 | Candidature introuvable |
| `APPLICATION_MISSION_NOT_PUBLISHED` | 409 | Mission pas en PUBLISHED |
| `APPLICATION_OWN_MISSION` | 409 | Candidature a sa propre mission |
| `APPLICATION_ALREADY_EXISTS` | 409 | Deja postule |
| `APPLICATION_ACCESS_DENIED` | 403 | Acces refuse |
| `APPLICATION_INVALID_TRANSITION` | 409 | Transition de statut interdite |
| **Notification** | | |
| `NOTIFICATION_NOT_FOUND` | 404 | Notification introuvable |
| **Rating** | | |
| `RATING_MISSION_NOT_COMPLETED` | 400 | Mission non terminee |
| `RATING_WINDOW_EXPIRED` | 400 | Fenetre de 7 jours depassee |
| `RATING_ALREADY_EXISTS` | 409 | Deja note |
| `RATING_UNAUTHORIZED` | 403 | Non autorise |
| `RATING_SELF_RATING` | 400 | Auto-notation interdite |
| **Generique** | | |
| `VALIDATION_FAILED` | 400 | Erreur de validation des champs |
| `CONFLICT` | 409 | Contrainte d'integrite violee |
| `CONCURRENT_MODIFICATION` | 409 | Modification concurrente, reessayer |
| `RATE_LIMIT_EXCEEDED` | 429 | Trop de requetes |

---

## 8. Rate limiting

| Pattern d'endpoint | Limite | Fenetre |
|--------------------|--------|---------|
| `POST /auth/otp/**` | 5 | 60 sec |
| `POST /auth/register`, `POST /auth/login` | 10 | 60 sec |
| `POST /applications` | 20 | 60 sec |

Reponse `429 Too Many Requests` avec le code `RATE_LIMIT_EXCEEDED`.

---

## 9. Valeurs de configuration

| Parametre | Valeur |
|-----------|--------|
| Duree access token | 15 minutes |
| Duree refresh token | 30 jours |
| Duree min mission | 60 minutes |
| Duree max mission | 120 minutes |
| Avance max reservation | 48 heures |
| Rayon recherche par defaut | 5 000 metres |
| Rayon recherche max | 20 000 metres |
| Workers max par mission | 5 |
| Commission plateforme | 10% |
| Remuneration min (XOF) | 500 |
| Remuneration min (GHS) | 5 |
| Fenetre de notation | 7 jours apres completion |
| Bruit coordonnees approx | +-200 metres |

---

## 10. Ordre d'integration recommande

1. **Auth** : OTP, register, login, refresh, logout — prerequis pour tout le reste
2. **Profil** : `GET /users/me` — afficher le profil, verifier les roles
3. **Langue, position et categories** : `PUT /users/me/language`, `PUT /users/me/location`, `PUT /users/me/categories` — configurer le profil
4. **Creation de mission** : `POST /missions` — flux client
5. **Mes missions** : `GET /missions/me/client`, `GET /missions/me/worker` — tableau de bord
6. **Recherche nearby** : `GET /missions/nearby` — recherche par proximite
7. **Feed Explorer** : `GET /missions/feed` — page d'exploration
8. **Detail mission** : `GET /missions/{id}` — page de detail
9. **Candidatures** : `POST /applications`, `GET /applications/me`, `GET /applications/mission/{id}` — flux de candidature
10. **Gestion candidatures** : `accept`, `reject`, `cancel` — decisions client/worker
11. **Lifecycle mission** : `start`, `complete`, `cancel` — transitions de statut
12. **Notifications REST** : `GET /notifications`, `PATCH read`, `POST read-all` — centre de notifications
13. **WebSocket** : connexion STOMP, abonnement `/user/queue/notifications` — temps reel
14. **Notation** : `POST /missions/{id}/ratings` — notation apres completion
15. **Maps** : utiliser `approximateLatitude`/`approximateLongitude` du `NearbyMissionResponse` pour les pins
