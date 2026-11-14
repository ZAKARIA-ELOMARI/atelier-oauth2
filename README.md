# Atelier OAuth2 / OpenID Connect — Keycloak + Spring Boot + React

> **Important** : ce code a été écrit sans pouvoir être compilé ni exécuté (Maven et npm
> étaient inaccessibles dans l'environnement de génération). Les fichiers ont été relus
> avec soin, mais si une erreur apparaît au premier lancement, envoyez-moi le message exact.

## Architecture

| Composant | Rôle | Port |
|---|---|---|
| Keycloak 26 (Docker) | Authorization Server / OpenID Provider | 8080 |
| `demandes-api` (Spring Boot 3.5) | Resource Server : valide les JWT | 8081 |
| `demandes-front` (React + Vite) | Client public, Authorization Code + PKCE | 5173 |

```
atelier-oauth2/
├── docker-compose.yml
├── demandes-api/        (pom.xml + src/main/...)
├── demandes-front/      (package.json + src/...)
└── tests/               (collection Postman)
```

> Arrêtez le projet 1 (JWT maison) avant : il utilise aussi les ports 8080 et 5173.

## 1. Lancer Keycloak

```bash
docker compose up -d
```

Console d'administration : http://localhost:8080 — `admin` / `admin` (le premier démarrage prend 30 à 60 s).

## 2. Configurer Keycloak (une seule fois)

1. **Realm** : menu déroulant en haut à gauche → *Create realm* → nom `enset-realm`.
2. **Realm roles** → *Create role* : `USER`, puis `ADMIN`.
3. **Clients** → *Create client* :
   - Client type : `OpenID Connect` — Client ID : `react-client`
   - Client authentication : **Off** (client public)
   - Standard flow : **On** — Direct access grants : **Off** (voir §6 pour Postman)
   - Valid redirect URIs : `http://localhost:5173/*`
   - Valid post logout redirect URIs : `http://localhost:5173/*`
   - Web origins : `http://localhost:5173`
   - Onglet *Advanced* → *Advanced settings* → **Proof Key for Code Exchange Code Challenge Method** = `S256` → Save.
4. **Users** → *Create user* (Keycloak 26 exige email, prénom et nom, sinon la connexion échoue avec « Account is not fully set up ») :

| Username | Email | Prénom / Nom | Email verified | Mot de passe | Rôles (onglet *Role mapping*) |
|---|---|---|---|---|---|
| `alice` | alice@enset.ma | Alice / Test | **On** | `alice123` (Temporary **Off**) | `USER` |
| `bob` | bob@enset.ma | Bob / Admin | **On** | `bob123` (Temporary **Off**) | `USER` + `ADMIN` |

Mot de passe : onglet *Credentials* → *Set password*.

## 3. Lancer l'API Spring Boot

**Option B (la plus simple)** : le `pom.xml` fourni est complet.

```bash
cd demandes-api
mvn spring-boot:run
```

(Le wrapper `mvnw` n'est pas inclus ; vous pouvez le générer avec `mvn -N wrapper:wrapper`.)

**Option A (VS Code + Spring Initializr)** : créez un projet Maven Java 17, Spring Boot 3.5.x,
Group `ma.formation`, Artifact `demandes-api`, dépendances *Spring Web*, *Spring Security*,
*OAuth2 Resource Server*. Puis copiez les 4 fichiers `.java` dans
`src/main/java/ma/formation/demandesapi/` (remplacez la classe `DemandesApiApplication`) et
`application.properties` dans `src/main/resources/`. Lancez ensuite la classe principale.

Test rapide : http://localhost:8081/api/public → `{"message":"API publique accessible"}`.
Keycloak doit être démarré **avant** l'API (l'API lit `issuer-uri` au démarrage).

## 4. Lancer le frontend

```bash
cd demandes-front
npm install
npm run dev
```

Ouvrir http://localhost:5173 → redirection vers Keycloak → connexion → retour sur l'application.

- `alice` : voit « Nouvelle demande », « Mes demandes », « Mon profil ».
- `bob` : voit en plus la section Administration avec les boutons **Valider** et **Refuser**.

## 5. Tests demandés (captures)

| # | Test | Attendu |
|---|---|---|
| 1 | `GET /api/public` sans token | 200 |
| 2 | `GET /api/demandes` sans token | 401 |
| 3 | `GET /api/admin/demandes` avec token d'alice | 403 |
| 4 | `GET /api/admin/demandes` avec token de bob | 200 |
| 5 | Token modifié (un caractère ajouté) | 401 |
| 6 | Connexion alice dans React | pas de section admin |
| 7 | Connexion bob dans React | section admin visible |

Extras couverts par la collection : création de demande, objet vide (400), PATCH par alice (403),
VALIDEE/REFUSEE par bob (200), statut invalide (400), id inexistant (404), `/api/profil` (200/401).

## 6. Obtenir des tokens pour Postman

Importez `tests/Demandes-OAuth2.postman_collection.json`. Deux méthodes :

- **Sans rien changer à Keycloak (recommandé)** : connectez-vous dans React, ouvrez les DevTools
  (F12) → onglet *Network* → cliquez une requête vers `localhost:8081` → *Request Headers* →
  copiez la valeur après `Bearer ` dans la variable de collection `aliceToken` (ou `bobToken`).
  Le token expire après 5 minutes par défaut.
- **Automatique** : activez temporairement *Direct access grants* sur `react-client`, puis lancez
  le dossier « 0 - Obtenir les tokens ». Désactivez-le ensuite.

Exécutez le dossier 1, puis 2, puis 3 dans l'ordre (le dossier 2 réutilise l'id de la demande créée).

## 7. Analyse du JWT

Dans la console du navigateur sur http://localhost:5173 :

```js
JSON.parse(atob(keycloak.token.split('.')[1]))
```

Claims à commenter : `iss` (émetteur, doit être `http://localhost:8080/realms/enset-realm`),
`sub` (identifiant unique, utilisé comme propriétaire des demandes), `exp`/`iat`,
`azp` (`react-client`), `preferred_username`, `realm_access.roles`.
Ne publiez jamais un vrai token dans le rapport ou sur GitHub.

## 8. Dépannage

| Symptôme | Cause / solution |
|---|---|
| « Account is not fully set up » | Renseigner email, prénom, nom et Email verified = On |
| `Port 8080/5173 already in use` | Arrêter le projet 1 |
| API : `Unable to resolve the Issuer` | Keycloak non démarré ou realm absent |
| 401 alors que le token est valide | `iss` différent (ex. `127.0.0.1` au lieu de `localhost`) : utiliser `localhost` partout |
| Erreur CORS dans le navigateur | Frontend lancé ailleurs que sur `http://localhost:5173` |
| « Invalid redirect uri » | Ajouter `http://localhost:5173/*` aux Valid redirect URIs / post logout |
| Chemin Windows avec accents | Placer le projet dans un chemin sans accents ni espaces |
| Demandes perdues | Stockage en mémoire : normal au redémarrage de l'API |

## 9. Travail supplémentaire inclus

1. Bouton **Refuser** (PATCH `REFUSEE`) à côté de Valider.
2. Endpoint `GET /api/profil` (id, username, rôles issus du JWT) + affichage dans React.
3. Tests de sécurité automatisés (collection Postman avec assertions).
