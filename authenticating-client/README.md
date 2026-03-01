# mTLS (Mutual TLS) avec Kafka

## Objectif
Ce module explique comment mettre en place **l’authentification mutuelle (mTLS)** entre les composants Kafka (brokers, producteurs et consommateurs) afin de résoudre les limites de sécurité d’une connexion TLS classique.

## Problématique
Avec **TLS standard** :
- Le **client peut vérifier l’identité du serveur** grâce à son certificat.
- Le **serveur ne peut pas identifier le client**, car celui-ci ne présente pas de certificat.

Cela pose un problème d’authentification dans des environnements distribués comme Kafka.

## Solution : Mutual TLS (mTLS)
Le **mTLS**, aussi appelé *two-way TLS*, permet une authentification **dans les deux sens** :
- Le serveur présente son certificat → vérifié par le client via un *truststore*.
- Le client présente son certificat → vérifié par le serveur via son propre *truststore*.

Ainsi, **chaque partie connaît précisément l’identité de l’autre**.

## Fonctionnement du handshake mTLS
1. Le serveur envoie son certificat au client.
2. Le client vérifie ce certificat avec le CA présent dans son truststore.
3. Le client envoie ensuite son propre certificat.
4. Le serveur vérifie l’authenticité du certificat client à l’aide de son truststore.

## Kafka et l’authentification
Kafka supporte deux mécanismes d’authentification :
- **mTLS**
- **SASL** (abordé dans un module ultérieur)

# SASL avec Kafka

## Objectif
Ce module présente **SASL (Simple Authentication and Security Layer)** comme alternative à **mTLS** pour l’authentification des composants Kafka, en mettant l’accent sur la réduction de la complexité opérationnelle liée à la gestion des certificats.

## Limites du mTLS
Bien que **mTLS** soit très sécurisé, il présente un inconvénient majeur :
- Chaque application nécessite son **propre certificat**
- Gestion, rotation et maintenance des certificats coûteuses en temps et en effort

Pour répondre à ce problème, Kafka propose **SASL** comme mécanisme d’authentification.

## Qu’est-ce que SASL ?
- **SASL** signifie *Simple Authentication and Security Layer*
- C’est une **spécification**, pas une implémentation
- Elle définit un cadre permettant d’authentifier des clients via différents mécanismes

Kafka implémente SASL via **JAAS**.

## Mécanismes SASL supportés par Kafka
Kafka prend en charge principalement les mécanismes suivants :

- **PLAIN** – simple, souvent utilisé pour les tests
- **SCRAM** – plus sécurisé, recommandé en production
- **GSSAPI** – basé sur Kerberos
- **OAUTHBEARER** – basé sur des tokens OAuth 2.0

> 🔎 **DIGEST-MD5** peut être utilisé avec ZooKeeper, mais il est de moins en moins pertinent car ZooKeeper disparaît progressivement de l’architecture Kafka.

## JAAS (Java Authentication and Authorization Service)
- Implémentation Java de SASL
- Introduite avec **Java 1.3** (plus de 20 ans)
- Basée sur le concept de **modules d’authentification pluggables**
- Utilisée par Kafka car le **protocole Kafka repose sur TCP**, ce qui limite l’usage de frameworks plus modernes

Kafka utilise **JAAS uniquement pour l’authentification**, pas pour l’autorisation.

## Fichiers de configuration JAAS
Kafka utilise uniquement des fichiers **`.conf`** pour SASL.

### Structure d’un fichier JAAS
Un fichier JAAS doit contenir :
- Une entrée **KafkaServer**
- Des accolades `{ }`
- Chaque entrée se termine par un point-virgule `;`

### Modules selon le mécanisme
À l’intérieur du bloc `KafkaServer`, on définit le module correspondant :

| Mécanisme SASL | Module JAAS |
|----------------|-------------|
| PLAIN          | `PlainLoginModule` |
| SCRAM          | `ScramLoginModule` |
| OAUTHBEARER    | `OAuthBearerLoginModule` |
| GSSAPI         | `Krb5LoginModule` |

## Listeners Kafka et SASL
Kafka propose quatre types de listeners :

- **PLAINTEXT**
- **SSL**
- **SASL_PLAINTEXT**
- **SASL_SSL**

### Bonnes pratiques
- **SASL_PLAINTEXT**
    - Pas de chiffrement
    - Acceptable uniquement en environnement de test ou de développement

- **SASL_SSL**
    - SASL combiné avec TLS
    - **Fortement recommandé en production**

## Conclusion
SASL offre une alternative flexible et plus simple à gérer que mTLS pour l’authentification Kafka. En combinant **SASL avec SSL**, on obtient un bon compromis entre **sécurité**, **maintenabilité** et **simplicité opérationnelle**, en particulier pour les environnements de production.

# SASL PLAIN avec Kafka

## Objectif
Ce module explique le fonctionnement et la configuration de **SASL PLAIN** dans Kafka, un mécanisme d’authentification simple basé sur le modèle **nom d’utilisateur / mot de passe**.

## Qu’est-ce que SASL PLAIN ?
- Mécanisme d’authentification **simple et direct**
- Basé sur des **identifiants statiques** (username / password)
- Le client envoie ses identifiants
- Le broker vérifie ces identifiants dans une **liste préconfigurée**

## Principe général dans Kafka
Kafka utilise **JAAS** pour configurer SASL PLAIN :
- Chaque application possède son **propre fichier JAAS**
- L’authentification repose sur le module `PlainLoginModule`

## Fichiers JAAS et rôles
### Côté Broker
- Entrée obligatoire : **KafkaServer**
- Contient :
    - Le module `PlainLoginModule`
    - La liste des **clients autorisés** avec leurs identifiants

### Côté Client (Producer, Consumer, Admin)
- Entrée obligatoire : **KafkaClient**
- Contient :
    - Le module `PlainLoginModule`
    - Les **identifiants du client**

## Déclaration des identifiants
### Sur le broker
Les utilisateurs autorisés sont déclarés sous la forme :
user_<nom_du_client> = <mot_de_passe>

    - Exemple : user_producer = producer-secret


Le broker peut :
- Déclarer plusieurs clients (producer, consumer, autres brokers)
- Servir à la fois de **serveur et de client**, ce qui explique la présence d’identifiants dans les fichiers JAAS des brokers

### Sur le client
Les identifiants sont fournis via deux propriétés :
- `username`
- `password`

Ces valeurs doivent **correspondre exactement** à celles déclarées côté broker.

## Points importants de sécurité
### Chiffrement des échanges
- Les identifiants sont transmis **sur le réseau**
- **SASL PLAIN doit impérativement être utilisé avec TLS** en production
- Recommandation : **SASL_SSL**

### Stockage des secrets
- Éviter le stockage des mots de passe en **clair sur disque**
- Par défaut, JAAS stocke les secrets dans les fichiers `.conf`

### Alternatives plus sécurisées
Kafka permet d’aller plus loin :
- Implémenter des **callback handlers personnalisés**
- Récupérer les identifiants depuis :
    - Un coffre-fort de secrets
    - Un service externe d’authentification
- Déléguer entièrement l’authentification à un système tiers
- Éviter les longues listes d’identifiants dans `broker_jaas.conf`

## Cas d’usage
- Environnements de **développement ou de test**
- Scénarios simples nécessitant une mise en place rapide
- Production possible **uniquement avec TLS** et une bonne gestion des secrets

## Conclusion
SASL PLAIN est un mécanisme d’authentification Kafka **facile à configurer**, mais **intrinsèquement sensible**. Il doit toujours être combiné avec **TLS** et idéalement renforcé par une gestion externe des identifiants pour un usage sécurisé en production.

# SASL SCRAM avec Kafka

## Objectif
Ce module présente **SASL SCRAM** comme un mécanisme d’authentification Kafka plus sécurisé que **SASL PLAIN**, reposant sur des échanges cryptographiques plutôt que sur la transmission directe des mots de passe.

## Qu’est-ce que SASL SCRAM ?
**SCRAM** signifie *Salted Challenge Response Authentication Mechanism*.

Principes clés :
- Le **client ne transmet jamais son mot de passe en clair**
- Le serveur envoie un **challenge**
- Le client ne peut répondre correctement **que s’il possède le bon mot de passe**
- Les mots de passe sont :
  - **hachés**
  - **salés**
  - stockés de manière sécurisée

## Algorithmes de hachage
Kafka supporte deux variantes SCRAM basées sur SHA-2 :
- **SCRAM-SHA-256**
- **SCRAM-SHA-512**

Les deux sont considérées comme **sécurisées** à ce jour.  
➡️ Le choix est libre, mais **doit être cohérent sur tous les brokers**.

## Configuration JAAS
Comme pour les autres mécanismes SASL, SCRAM utilise **JAAS**.

### Entrées obligatoires
- **KafkaServer** pour les brokers
- **KafkaClient** pour les producteurs, consommateurs et outils admin

### Module d’authentification
Pour SCRAM, on utilise :


Contrairement à SASL PLAIN :
- Les **listes complètes d’identifiants ne sont pas stockées dans les fichiers JAAS**
- Elles sont stockées dans **ZooKeeper**

Les fichiers JAAS contiennent uniquement :
- Le nom d’utilisateur
- Le mot de passe du client ou du broker

## Stockage des identifiants
- Les identifiants SCRAM sont stockés dans **ZooKeeper**
- Cela implique :
  - ZooKeeper devient un **élément critique de sécurité**
  - Il doit impérativement être **sécurisé**

## Considérations de sécurité
Lors de l’utilisation de SASL SCRAM, il est essentiel de respecter ces règles :

1. **Sécuriser ZooKeeper**
  - Les identifiants y sont stockés
  - Une compromission expose l’authentification Kafka

2. **Toujours utiliser TLS en production**
  - Utiliser **SASL_SSL**
  - Protège contre :
    - Attaques par dictionnaire
    - Attaques par force brute

3. **Utiliser des mots de passe forts**
  - Réduit l’impact d’une fuite éventuelle
  - Renforce la résistance aux attaques hors ligne

4. **Callback handlers personnalisés**
  - Possibilité de :
    - Ne pas stocker les identifiants dans ZooKeeper
    - Utiliser un coffre-fort ou un service externe
    - Déléguer complètement l’authentification

## Mise en place pratique (résumé)
### Étapes principales
1. Créer les fichiers JAAS SCRAM pour brokers et clients
2. Configurer Kafka avec :
  - `SCRAM-SHA-256` ou `SCRAM-SHA-512`
  - `SASL_MECHANISM_INTER_BROKER_PROTOCOL`
3. Démarrer ZooKeeper
4. Créer les identifiants via les scripts Kafka (`kafka-configs`) 
      ./kafka-configs.sh --zookeeper zookeeper-1:2281 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=broker-secret]' --entity-type users --entity-name broker
      ./kafka-configs.sh --zookeeper zookeeper-1:2281 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=producer-secret]' --entity-type users --entity-name producer
      ./kafka-configs.sh --zookeeper zookeeper-1:2281 --alter --add-config 'SCRAM-SHA-256=[iterations=8192,password=consumer-secret]' --entity-type users --entity-name consumer
5. Déployer les brokers
6. Configurer producteurs et consommateurs avec :
  - SASL SCRAM
  - JAAS
  - Option JVM `-Djava.security.auth.login.config`
7. Démarrer les clients et valider les échanges

## Points d’attention
- Les brokers sont **à la fois clients et serveurs**
- Chaque broker doit avoir ses propres identifiants SCRAM
- Chaque client (producer, consumer) doit avoir :
  - Un utilisateur distinct
  - Des identifiants stockés dans ZooKeeper

## Avantages de SASL SCRAM
- Plus sécurisé que SASL PLAIN
- Pas de mot de passe transmis sur le réseau
- Compatible avec TLS
- Configuration cohérente avec les autres mécanismes SASL

## Conclusion
**SASL SCRAM** est un excellent compromis entre **sécurité**, **simplicité** et **robustesse**. Associé à **TLS** et à un ZooKeeper sécurisé, il constitue une solution solide pour l’authentification Kafka en production, tout en restant plus simple à gérer que mTLS.


# 🔐 Authentification Kafka avec SASL OAUTHBEARER

## 📌 Introduction

Le mécanisme **SASL OAUTHBEARER** dans Apache Kafka est basé sur le framework OAuth.  
Il permet à des applications tierces d’obtenir un accès limité à des ressources protégées.

Dans le contexte Kafka :

- Applications tierces → Clients Kafka (Producer, Consumer, Brokers)
- Ressources protégées → Principalement les topics Kafka

Comme les autres mécanismes SASL (PLAIN, SCRAM), **SASL OAUTHBEARER nécessite une configuration JAAS**.
Le module obligatoire à déclarer est :  org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule
---

## 🧾 Les Claims OAuth dans Kafka

OAuth repose sur des **claims** présents dans les JSON Web Tokens (JWT).
Kafka supporte trois types de claims :
- String
- Number
- List

Dans cet exemple, nous utilisons un claim String appelé : sub


### 🔎 Le claim `sub`

- `sub` signifie *subject*
- Il représente l’identité (principal) de l’application
- Exemples :
    - admin
    - broker
    - producer
    - consumer

Il est possible de modifier le nom du claim principal via : unsecuredLoginPrincipalClaimName

---

## ⚠️ Implémentation par défaut NON sécurisée

L’implémentation SASL OAUTHBEARER fournie par Kafka :

- Génère des JWT non signés
- Valide des tokens sans vérifier leur signature
- N’effectue aucune vérification d’authenticité

👉 Tout JWT correctement formaté sera accepté.

Cette implémentation :

- Peut convenir pour du développement
- Ne doit jamais être utilisée en production

### 🔒 En production vous devez :

- Activer TLS (SASL_SSL)
- Valider les signatures JWT
- Implémenter des Server Callback Handlers personnalisés
- Idéalement utiliser un fournisseur OAuth (Keycloak, Auth0…)

---

# 🧪 Mise en place d’un environnement de démonstration

## 1️⃣ Configuration JAAS

### Broker (broker_jaas.conf)
KafkaServer {
org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
unsecuredLoginStringClaim_sub="broker";
};

### Producer (producer_jaas.conf)
KafkaClient {
org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
unsecuredLoginStringClaim_sub="producer";
};


### Consumer (consumer_jaas.conf)
KafkaClient {
org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
unsecuredLoginStringClaim_sub="consumer";
};


---

## 2️⃣ Configuration Docker (brokers)
Dans le docker-compose :
SASL_ENABLED_MECHANISMS=OAUTHBEARER
SASL_MECHANISM_INTER_BROKER_PROTOCOL=OAUTHBEARER

Ces paramètres doivent être appliqués à tous les brokers.

---

## 3️⃣ Déploiement

Arrêter l’ancien cluster :
Lancer la configuration OAUTHBEARER : docker-compose -f docker-compose-oauth.yml up
Vérifier les logs : 

⚠️ Les brokers s’authentifient automatiquement car aucun contrôle réel de signature n’est effectué.

---

# 👥 Configuration des Clients

Les applications doivent être démarrées avec le flag JVM : -Djava.security.auth.login.config=/chemin/vers/jaas.conf


Dans le code Java (Producer ou Consumer) :

```java
props.put("security.protocol", "SASL_SSL");
props.put("sasl.mechanism", "OAUTHBEARER");