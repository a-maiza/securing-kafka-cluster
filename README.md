# 🔐 Sécurisation de ZooKeeper (SSL / TLS)

Ce document décrit les étapes nécessaires pour sécuriser un cluster **ZooKeeper** existant à l’aide de **SSL/TLS**, sans perte de données, en utilisant la stratégie de **rolling update**.

---

## 📌 Contexte

ZooKeeper est utilisé par Kafka pour la coordination.  
Par défaut, toutes les communications ZooKeeper sont en **clair (plain text)**, ce qui pose des risques de sécurité.

Deux types de communications doivent être sécurisés :
1. **Communication interne du quorum ZooKeeper** (entre serveurs ZooKeeper)
2. **Communication ZooKeeper ↔ Clients** (Kafka brokers)

---

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

## 🔐 Sécurisation du quorum ZooKeeper (Serveur ↔ Serveur)

### 🟢 État initial (non sécurisé)
- Communication en **plain text**
- Aucun chiffrement

