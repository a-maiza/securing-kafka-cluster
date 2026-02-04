# Securing a Kafka Cluster – Introduction & Foundations

## 📌 Objectif du module

Ce module introduit les **fondements de la sécurité dans un cluster Apache Kafka**, en mettant l’accent sur le **chiffrement des communications**, 
les **certificats**, et l’architecture de confiance nécessaire pour sécuriser l’ensemble des composants Kafka.

L’objectif principal est de comprendre **pourquoi** et **comment** sécuriser Kafka dans un contexte moderne basé sur le **Zero Trust Model**.

---

## 🔐 Pourquoi sécuriser Kafka ?

Traditionnellement, Kafka est déployé profondément à l’intérieur des réseaux d’entreprise et protégé par une défense périmétrique (firewalls, réseaux privés, etc.).  
Cependant, cette approche présente une faille majeure :

> **Si un attaquant pénètre le réseau interne, Kafka devient totalement exposé.**

### Zero Trust Model

Le **Zero Trust Model** repose sur un principe simple :
- Ne faire confiance à **aucun composant**, qu’il soit interne ou externe
- **Toujours vérifier l’identité** et les **droits d’accès**

Kafka, en tant que système critique d’échange de données, doit donc être sécurisé **nativement**.

---

## 🧰 Prérequis du cours

Avant de suivre ce module, il est recommandé d’avoir :

- ✅ Des bases solides en **Apache Kafka**
    - Topics, producers, consumers, brokers
- ✅ Une familiarité avec **Docker**
    - Kafka et ses composants seront déployés via des conteneurs
- ✅ Des notions de **Java (version 11+)**
    - Des applications simples seront écrites (syntaxe basique uniquement)

⚠️ Le module **n’explique pas les bases de Kafka**.

---

## 🔒 Problème n°1 : Sécuriser les communications

Sans sécurité :
- Un attaquant ayant accès au réseau peut **écouter toutes les communications**
- Les données transitent en clair

### Solution : Chiffrement avec TLS

Le chiffrement garantit que :
- Les données sont **illisibles pendant le transport**
- Seuls les composants légitimes peuvent les déchiffrer

> ⚠️ Le terme **SSL** est souvent utilisé dans Kafka, mais il fait en réalité référence à **TLS**, le protocole moderne.

---

## 🔑 Rappels de cryptographie

### 1. Chiffrement symétrique
- Une seule clé pour chiffrer et déchiffrer
- Rapide, mais pose des problèmes de partage de clé

### 2. Chiffrement asymétrique
- Une **clé publique** (chiffrement)
- Une **clé privée** (déchiffrement)
- Base du fonctionnement de TLS

### 3. Certificats & PKI (Public Key Infrastructure)

Les certificats permettent de :
- Vérifier l’identité d’un serveur ou d’une application
- Éviter les attaques de type *man-in-the-middle*

Un **Certificate Authority (CA)** :
- Signe les certificats
- Est reconnu comme source de confiance

---

## 🗄️ Keystore & Truststore (Java)

### Truststore
- Contient les **certificats des CA**
- Utilisé par les **clients**
- Sert à vérifier l’identité des serveurs

### Keystore
- Contient :
    - La **clé privée**
    - Le **certificat de l’application**
    - Le **certificat de la CA**
- Utilisé par les **serveurs**

---

## 🧩 Architecture Kafka sécurisée

Un cluster Kafka sécurisé implique plusieurs types de communications :

| Composants | Type de communication |
|----------|----------------------|
| Producers ↔ Brokers | Client ↔ Serveur |
| Consumers ↔ Brokers | Client ↔ Serveur |
| Brokers ↔ Brokers | Inter-broker |
| ZooKeeper ↔ ZooKeeper | Quorum |
| Brokers ↔ ZooKeeper | Zoo-client |

### Implications

- **ZooKeeper et Brokers**
    - Agissent à la fois comme **clients et serveurs**
    - Nécessitent **Keystore + Truststore**
- **Kafka Clients**
    - Agissent uniquement comme **clients**
    - Nécessitent uniquement un **Truststore**

---

## 🧪 Démo : Création des Keystores et Truststores

### Outils requis
- `keytool` (Java)
- `openssl`

### Étapes principales

1. **Création du Truststore**
    - Import du certificat de la CA
2. **Création du Keystore**
    - Génération d’une paire de clés RSA
    - Définition du *Distinguished Name (DN)*
3. **Création d’une CSR (Certificate Signing Request)**
4. **Signature du certificat par la CA**
5. **Import dans le Keystore**
    - Certificat de la CA
    - Certificat applicatif signé
6. **Vérification du contenu du Keystore**

Chaque broker nécessite :
- Un **Keystore**
- Un **Truststore**

---

## ⚙️ Automatisation

Pour éviter de répéter manuellement ces étapes :
- Des scripts sont fournis :
    - `generate-keystore`
    - `generate-truststore`
- Un seul paramètre requis :
    - Le **Common Name (CN)** de l’application

---

## ✅ Résultat attendu

À la fin de ce module :
- Les bases de la **sécurité Kafka** sont comprises
- Les concepts de **TLS, PKI, certificats, keystores et truststores** sont maîtrisés
- L’infrastructure est prête pour sécuriser :
    - Les brokers
    - ZooKeeper
    - Les clients Kafka

---

## 🚀 Prochaine étape

Appliquer ces concepts pour :
- Sécuriser les communications Kafka en pratique
- Activer TLS sur l’ensemble du cluster
- Ajouter authentification et autorisation

---

1️⃣ `KAFKA_BROKER_ID: 1`  

## 👉 À quoi ça sert ?  
Identifiant unique du broker dans le cluster.

## 🔎 Détails
Chaque broker doit avoir un ID différent

## Sert pour :
    - l’élection de leader
    - la réplication
    - le metadata store

## 📌 Exemple
    Broker	ID
    broker-1	1
    broker-2	2
    broker-3	3

2️⃣ `KAFKA_ZOOKEEPER_CONNECT : KAFKA_ZOOKEEPER_CONNECT: zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181`

## 👉 Rôle
Permet au broker de se connecter au quorum Zookeeper.

## 🔎 Détails
## Zookeeper gère :

    - metadata du cluster
    - leaders des partitions
    - ISR (In-Sync Replicas)
    - health du cluster

➡️ Tous les brokers doivent avoir exactement la même valeur.

3️⃣ `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP : INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT`

## 👉 Rôle
Associe chaque listener logique à un protocole réseau.

Listener	Protocole
INTERNAL	PLAINTEXT
EXTERNAL	PLAINTEXT

## 🔐 Signification de PLAINTEXT

    - Pas de chiffrement
    - Pas d’authentification
    - OK pour dev / test
    - ❌ pas pour prod

4️⃣ `KAFKA_LISTENERS : INTERNAL://0.0.0.0:29091,EXTERNAL://0.0.0.0:9091`

## 👉 Rôle

Définit où le broker écoute réellement.

## 🔎 Détails

    - 0.0.0.0 → toutes les interfaces
    - 29091 → port interne Docker
    - 9091 → port exposé vers l’hôte
    - 📡 Le broker ouvre ces ports.

5️⃣ `KAFKA_ADVERTISED_LISTENERS : INTERNAL://broker-1:29091,EXTERNAL://localhost:9091`

## 👉 Rôle

Définit les adresses que Kafka annonce aux clients.

## 🧠 Très important

Kafka n’utilise pas LISTENERS pour dire aux clients où se connecter , il utilise ADVERTISED_LISTENERS

🌍 Qui voit quoi ?
Client	Adresse reçue
Broker ↔ Broker	broker-1:29091
Client local	localhost:9091


6️⃣ `KAFKA_INTER_BROKER_LISTENER_NAME : INTERNAL`

## 👉 Rôle
Indique quel listener est utilisé pour la communication entre brokers.

## 🔄 Utilisé pour :

    - réplication
    - ISR
    - leader election
    - metadata exchange

## ❗ Règle

Doit être un nom présent dans ADVERTISED_LISTENERS

En Docker → toujours INTERNAL

7️⃣ `KAFKA_DEFAULT_REPLICATION_FACTOR: 3`
## 👉 Rôle

Facteur de réplication par défaut lors de la création d’un topic.

## 🔎 Détails

    - 3 copies de chaque partition
    - 1 leader + 2 followers
    - Tolérance à 2 brokers down (lecture)

8️⃣ `KAFKA_MIN_INSYNC_REPLICAS: 2`
##  👉 Rôle

Nombre minimum de réplicas synchronisées pour accepter une écriture.

##  🛡️ Sécurité
Avec :
    - RF = 3
    - min.insync = 2

➡️ Kafka garantit aucune perte de message si 1 broker tombe.

🔐 Côté producer

Doit être utilisé avec :
acks=all

9️⃣ `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3`
## 👉 Rôle
Facteur de réplication du topic interne :
__consumer_offsets

## 🔎 Pourquoi c’est critique ?

Stocke les offsets des consumers

Si perdu → consumers repartent de zéro

➡️ Toujours ≥ 3 dans un cluster à 3 brokers.



