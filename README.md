# ADRIA Bank — TP

Application simple en **React + Vite**, **Java 17 / Spring Boot 3.5**, Spring Cloud et Spring AI avec **Gemini**. Pas de rapport, de déploiement ni de détection de fraude dans cette version.

## Démarrage rapide sous Windows

Prérequis : Java 17+, Maven 3.9+, Node.js 22.12+ ou 24.

### Lancer le projet complet après extraction du ZIP

Extraire tout le dossier `ADRIA-Bank`, ouvrir PowerShell dans ce dossier, puis exécuter :

```powershell
.\LANCER.ps1
```

Ce lanceur installe les dépendances React, compile les modules Maven, exécute les tests et démarre le backend **et** le frontend. Internet est nécessaire au premier lancement pour télécharger les dépendances. Ouvrir ensuite **http://localhost:15173**. Compter quelques minutes au premier lancement.

Pour activer également le chatbot Gemini :

```powershell
$env:GEMINI_API_KEY = 'VOTRE_CLE_GEMINI'
.\LANCER.ps1 -WithAi
```

La clé privée n'est pas incluse dans l'archive. Les outils MCP locaux ne nécessitent aucune clé. Sans `-WithAi`, les comptes, paiements et virements fonctionnent normalement.

Pour arrêter toute l'application : ` .\ARRETER.ps1 `. Pour les lancements suivants, sans modification du code : ` .\LANCER.ps1 -SkipBuild ` (ajouter `-WithAi` pour l'assistant). Si une ancienne copie tourne déjà sur cet ordinateur, l'arrêter depuis son propre dossier avant de lancer la nouvelle copie.

### Lancer avec Docker Desktop

Prérequis : Docker Desktop démarré. Java, Maven et Node.js ne sont pas nécessaires. Arrêter d'abord une instance locale (` .\ARRETER.ps1 `), car les ports sont les mêmes.

```powershell
docker compose up -d --build
```

Ouvrir **http://localhost:15173**. Le premier build télécharge les dépendances et prend quelques minutes. Les tests ne sont pas exécutés dans Docker.

**Connexion (Keycloak).** Avec Docker, l'application redirige vers une page de connexion Keycloak aux couleurs d'ADRIA Bank, et la gateway refuse toute requête `/api` sans jeton (HTTP 401).

| Utilisateur | Mot de passe |
|---|---|
| `demo` | `demo123` |
| `conseiller` | `conseiller123` |

Console d'administration : **http://localhost:18180/admin** (`admin` / `admin`), realm `adria-bank`. Le realm, les clients et les utilisateurs sont importés depuis `keycloak/realm/` au premier démarrage ; le thème de connexion se trouve dans `keycloak/themes/adria/`. Pour appeler l'API à la main, obtenir un jeton avec le client `adria-cli` :

```powershell
$t = Invoke-RestMethod -Method POST http://localhost:18180/realms/adria-bank/protocol/openid-connect/token -Body @{ grant_type='password'; client_id='adria-cli'; username='demo'; password='demo123' }
Invoke-RestMethod http://localhost:19080/api/accounts -Headers @{ Authorization = "Bearer $($t.access_token)" }
```

Avec `LANCER.ps1`, Keycloak n'est pas lancé : l'application reste accessible sans connexion, comme avant.

Options (copier `.env.example` en `.env` et le remplir) :

```powershell
docker compose --profile ai up -d --build      # chatbot Gemini, GEMINI_API_KEY dans .env
docker compose --profile kafka up -d --build   # Kafka + event-consumer, KAFKA_ENABLED=true dans .env
```

Logs : `docker compose logs -f transaction-service`. Arrêt : `docker compose down` (ajouter les mêmes `--profile` si utilisés). Les bases H2 sont en mémoire : les données repartent de zéro à chaque redémarrage. Pour recharger des clients, paiements et virements de démonstration : ` .\scripts\seed-data.ps1 ` (fonctionne aussi avec `LANCER.ps1`).

### Lancement séparé du backend et du frontend

Depuis la racine, dans PowerShell :

```powershell
.\scripts\start-local.ps1 -WithAi
```

Le script compile, lance les tests, puis démarre Eureka, Config, les comptes, les transactions, la gateway et le chatbot. La clé Gemini est lue depuis `GEMINI_API_KEY`, ou depuis le fichier local ignoré par Git `.local/gemini-key.txt`. Pour démarrer sans assistant, omettre `-WithAi`. Après une compilation réussie, ajouter `-SkipBuild` pour gagner du temps.

Dans un autre terminal :

```powershell
cd frontend
npm install
npm run dev
```

Ouvrir **http://localhost:15173**. Attendre environ 30 secondes si les routes de la gateway ne sont pas encore disponibles.

Arrêter les services lancés par le script :

```powershell
.\scripts\stop-local.ps1
```

Le frontend s'arrête avec `Ctrl+C`. Les logs du backend se trouvent dans `.local/`.

## Modules

| Module | Port | Rôle |
|---|---:|---|
| discovery-service | 19761 | Annuaire Eureka |
| config-service | 19888 | Configuration partagée, plafond de 1000 |
| account-service | 19081 | Comptes, soldes, statuts ; REST, GraphQL, MCP |
| transaction-service | 19082 | Paiements, virements, OpenFeign, Resilience4J ; REST, GraphQL, MCP |
| gateway-service | 19080 | Point d'entrée du frontend |
| chatbot-service | 19083 | Gemini et outils MCP ; client REST |
| event-consumer | 19084 | Journalisation des événements Kafka |
| frontend | 15173 | Comptes, paiements, virements, assistant |
| keycloak (Docker) | 18180 | Connexion, jetons JWT vérifiés par la gateway |

Les contrôleurs REST, GraphQL et les outils MCP utilisent la même couche `service`. Les données de démonstration comprennent deux comptes MAD et un compte EUR. Les bases H2 sont en mémoire : arrêter et relancer **les deux services métier ensemble** réinitialise la démonstration.

## Tester

```powershell
mvn test
cd frontend
npm run build
```

Les exemples REST et GraphQL sont dans `requests.http`, utilisable avec HTTP Client d'IntelliJ ou REST Client de VS Code. Pour tester rapidement : créer un paiement, vérifier le nouveau solde, effectuer un virement, puis bloquer un compte et essayer une opération. Un montant nul, négatif, supérieur au plafond ou supérieur au solde est refusé.

GraphiQL : `http://localhost:19081/graphiql` et `http://localhost:19082/graphiql`. Les montants GraphQL sont des chaînes décimales, par exemple `"250.00"`, pour conserver leur précision.

Les serveurs MCP utilisent Streamable HTTP : `http://localhost:19081/mcp` et `http://localhost:19082/mcp`. Ils ne demandent pas de clé API dans ce TP. Le chatbot se connecte aux deux serveurs au démarrage ; lancer les comptes et les transactions avant lui. Exemple : « Liste mes comptes avec leurs soldes ». Pour un paiement, préciser le compte, le montant, la devise, la catégorie et le pays. Chaque demande est indépendante, sans mémoire conversationnelle.

## Gemini

Pour fournir ou remplacer une clé, définir `GEMINI_API_KEY` dans le terminal avant de lancer le script. Ne jamais mettre une clé dans le code ou dans Git. Le modèle par défaut est `gemini-3.1-flash-lite`, modifiable avec `GEMINI_MODEL`. Le frontend ne reçoit jamais la clé. Aucun RAG n'est nécessaire : les données bancaires sont obtenues en direct via MCP.

## Kafka, lorsque le broker est disponible

Kafka est désactivé par défaut afin que le TP démarre sans infrastructure supplémentaire. Avec un broker existant :

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
.\scripts\start-local.ps1 -SkipBuild -WithAi -WithKafka
```

Arrêter les services existants avant cette relance. Après validation SQL d'une transaction, un événement `TransactionCreated` est publié dans `bank.transactions.created`, avec `accountId` comme clé. Le consumer affiche le JSON reçu sans modifier les données. L'installation du broker et Docker sont laissés pour la partie déploiement.

## Choix de simplicité du TP

- Une transaction représente un paiement sortant. Un virement ajoute `targetAccountId` et déplace les deux soldes dans une transaction SQL de `account-service`.
- Les montants utilisent `BigDecimal`, y compris le solde. Des verrous SQL protègent les écritures concurrentes sur les comptes.
- Resilience4J retourne une erreur explicite si le service des comptes est indisponible ; aucun solde fictif et aucune répétition automatique d'un débit.
- Pas de transaction distribuée entre les deux bases : une panne après le débit distant peut laisser l'historique incohérent. Vérifier les soldes avant de répéter une opération interrompue. Une saga et l'idempotence seraient des améliorations de production.
- La publication Kafka est après commit, sans outbox persistante : un événement peut être perdu en cas de panne. L'erreur est journalisée.
- Démonstration locale sans authentification Keycloak, sans gestion des droits et sans accès à une banque réelle. Les routes internes ne passent pas par la gateway. L'intégration Keycloak reste à faire.

Références techniques : [Spring Cloud](https://spring.io/projects/spring-cloud/), [Spring AI avec Gemini](https://docs.spring.io/spring-ai/reference/1.1/api/chat/google-genai-chat.html), [MCP Spring AI](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-server-boot-starter-docs.html), [Vite](https://vite.dev/guide/).
