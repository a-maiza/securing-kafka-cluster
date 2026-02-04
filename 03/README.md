# Sécurisation de ZooKeeper et Kafka Brokers avec TLS (Rolling Update)

## Objectif du module

Ce module explique comment :
- Sécuriser la communication **ZooKeeper ↔ ZooKeeper (Quorum)** avec TLS
- Sécuriser la communication **Kafka Brokers ↔ ZooKeeper** avec TLS
- Migrer un cluster existant **sans perte de données ni interruption de service**
- Utiliser la technique de **Rolling Update**

---

## Prérequis

- Cluster ZooKeeper existant (3 nœuds recommandé)
- Cluster Kafka existant (3 brokers)
- Keystores et Truststores déjà créés (JKS ou PKCS12)
- Docker & Docker Compose (pour la démo)
- Java 11 ou supérieur (pour les clients Kafka)
- Accès aux fichiers de configuration :
    - `zookeeper.properties` (Kafka distribution)
    - ou `zoo.cfg` (ZooKeeper standalone)

---

## 📌 Contexte

ZooKeeper est utilisé par Kafka pour la coordination.  
Par défaut, toutes les communications ZooKeeper sont en **clair (plain text)**, ce qui pose des risques de sécurité.

Deux types de communications doivent être sécurisés :
1. **Communication interne du quorum ZooKeeper** (entre serveurs ZooKeeper)
2. **Communication ZooKeeper ↔ Clients** (Kafka brokers)

---

## Concepts clés
## 🔄 Principe fondamental : Rolling Update

Un **rolling update** consiste à :
- Redémarrer **un seul nœud à la fois**
- Éviter d’arrêter plusieurs serveurs ZooKeeper simultanément
- Garantir **zéro perte de données**

⚠️ Redémarrer plusieurs nœuds ZooKeeper en même temps peut provoquer une corruption ou une perte de données.

---

## 🗂️ Fichiers de configuration

Selon l’installation :
- `zookeeper.properties` (ZooKeeper fourni avec Kafka)
- `zoo.cfg` (ZooKeeper vanilla)

---

## 🔐 Étape 1 Sécurisation du quorum ZooKeeper (ZooKeeper ↔ ZooKeeper)

### 🟢 État initial (non sécurisé)
Par défaut :
- Communication en **plain text**
- Aucune encryption
- Port unique

Objectif :
👉 Migrer progressivement vers **TLS pour le quorum**

---

### 🟡 Étape 1.1 : Préparation à la migration (1er rolling update - Préparation TLS)
ZK1 ---- plain text ---- ZK2 ---- plain text ---- ZK3

### Actions
Sur **chaque ZooKeeper**, un par un :
- Ajouter les **KeyStores** et **TrustStores**
- Modifier la configuration :
    - `portUnification = true`
    - `serverCnxnFactory = NettyServerCnxnFactory`
- Redémarrer le ZooKeeper

### Propriétés à ajouter
### properties
    - `sslQuorum=false`
    - `portUnification=true`
    - `serverCnxnFactory=org.apache.zookeeper.server.NettyServerCnxnFactory`
    - `
    - `ssl.quorum.keyStore.location=/security/zookeeper.keystore.jks`
    - `ssl.quorum.keyStore.password=********`
    - `ssl.quorum.trustStore.location=/security/zookeeper.truststore.jks`
    - `ssl.quorum.trustStore.password=********`
    - `ssl.hostnameVerification=false`

➡️ `portUnification` permet d’accepter **SSL et plain text sur le même port** Nécessaire pour une migration sans coupure
➡️ Netty est requis pour les connexions sécurisées


---

### 🔵 Étape 1.2 : Activation du SSL Quorum (2e rolling update - Activation TLS Quorum)
ZK1 ---- SSL + plain text ---- ZK2 ---- SSL + plain text ---- ZK3

- Activer le chiffrement du quorum :
    - `sslQuorum = true`

➡️ Les ZooKeepers communiquent désormais en TLS
➡️ Toujours compatible avec les anciens nœuds grâce à portUnification

---

### 🔴 Étape 1.3 : Finalisation (3e rolling update - Nettoyage)
ZK1 ---- SSL ---- ZK2 ---- SSL ---- ZK3

- Désactiver :
    - `portUnification = false`

📌 Cette option ne doit être utilisée **que pendant la migration**

➡️ Après migration, seul TLS est autorisé
➡️ Meilleure sécurité
➡️ Configuration finale recommandée

---

## 🔗 Étape 2 Sécurisation ZooKeeper ↔ Kafka (ZooKeeper ↔ Kafka Brokers)
Maintenant que le quorum est sécurisé, il faut :
👉 Sécuriser la communication clients ZooKeeper (brokers Kafka)

### 🟡 Étape 2.1 : Configuration ZooKeeper (port client TLS)

### Actions côté ZooKeeper
- Nouveau **rolling update**
- Ouverture d’un **port client sécurisé (TLS)**

### Propriétés à ajouter
### properties
    - `secureClientPort=2281
    - `ssl.keyStore.location=/security/zookeeper.keystore.jks
    - `ssl.keyStore.password=********
    - `ssl.trustStore.location=/security/zookeeper.truststore.jks
    - `ssl.trustStore.password=********
    - `ssl.hostnameVerification=false`

➡️ `ssl.hostnameVerification=false` est utilisé pour éviter les erreurs TLS lorsque le nom du serveur (hostname) ne correspond pas exactement au certificat, ce qui est fréquent avec ZooKeeper, Docker ou lors d’une migration TLS, tout en conservant le chiffrement de la communication.
➡️ Appliquer ces propriétés
➡️ Redémarrer chaque ZooKeeper un par un avec la commande `docker-compose up -d --no-deps --build zookeeper-x`



### 🔵 Étape 2.2 : Configuration Kafka Brokers
### Actions côté Kafka Brokers
- Utiliser les **KeyStores / TrustStores**
- Activer le client SSL ZooKeeper
- Modifier :
    - `zookeeper.ssl.client.enable = true`
    - `clientCnxnSocket = Netty`

### Propriétés à ajouter
### properties
    - `zookeeper.ssl.client.enable=true`
    - `zookeeper.clientCnxnSocket=org.apache.zookeeper.ClientCnxnSocketNetty`
    - `zookeeper.ssl.keystore.location=/security/kafka.keystore.jks`
    - `zookeeper.ssl.keystore.password=********`
    - `zookeeper.ssl.truststore.location=/security/kafka.truststore.jks`
    - `zookeeper.ssl.truststore.password=********`
    - `zookeeper.connect=zookeeper-1:2281,zookeeper-2:2281,zookeeper-3:2281`

➡️ Monter les keystores/truststores via volumes Docker ou filesystem
➡️ Redémarrer chaque broker un à un avec la commande `docker-compose up -d --no-deps --build broker-x` 
---

## ✅ État final (entièrement sécurisé)

- 🔐 Quorum ZooKeeper chiffré (SSL)
- 🔐 Communication Kafka ↔ ZooKeeper chiffrée (TLS)
- 🔄 Migration sans interruption de service
- ⚙️ Netty utilisé pour toutes les connexions sécurisées

---

## 🧠 Points clés à retenir

- Toujours utiliser des **rolling updates**
- `portUnification` est **temporaire**
- SSL/TLS doit être activé **progressivement**
- Netty est obligatoire pour les connexions sécurisées

---

## 📘 Résumé rapide

| Élément | Sécurisé |
|------|--------|
| Quorum ZooKeeper | ✅ SSL |
| ZooKeeper ↔ Kafka | ✅ TLS |
| Rolling Update | ✅ Oui |
| PortUnification | ❌ Après migration |


 
✍️ *Document conçu pour servir de référence lors d’une migration ZooKeeper sécurisée.*


