# Kafka Listeners, TLS et Chiffrement de bout en bout

Ce module explique comment sécuriser **toutes les communications Kafka**, depuis les échanges réseau (clients ↔ brokers, brokers ↔ brokers) jusqu’au **chiffrement de bout en bout des données**. Il combine concepts théoriques et démonstrations pratiques.

---

## 1. Kafka Listeners : principes fondamentaux

Les brokers Kafka exposent des **listeners**, c’est-à-dire des points d’entrée réseau auxquels les clients et les autres brokers se connectent.

Un listener est défini par :
- **un nom**
- **un port**
- **un type de sécurité**

### Types de listeners supportés
- `PLAINTEXT` : communication non chiffrée
- `SSL` : communication chiffrée via TLS
- `SASL_PLAINTEXT` (abordé plus tard)
- `SASL_SSL` (abordé plus tard)

Un broker peut exposer **plusieurs listeners simultanément**, par exemple :
- `PLAINTEXT` sur le port 9092
- `SSL` sur le port 9192

👉 Tous les brokers du cluster doivent avoir **la même configuration de listeners**, sinon le cluster peut se comporter de manière imprévisible.

---

## 2. Configuration des listeners Kafka

### Propriétés clés
- `listeners` Déclare les listeners et les ports (réseau interne).
- `listener.security.protocol.map` Associe chaque listener à son type de sécurité.
- `advertised.listeners` Adresses exposées aux clients (souvent différentes en cloud).
- `listener.name.<nom>.ssl.*` Permet de configurer chaque listener individuellement.

### Communication inter-brokers
Deux approches possibles (une seule doit être utilisée) :
- `security.inter.broker.protocol` Définit le protocole (ex: `SSL`)
- `inter.broker.listener.name` Sélectionne explicitement le listener à utiliser

⚠️ Il doit exister **au moins un listener compatible** avec la configuration choisie.

---

## 3. Impact des performances

La sécurité a un coût :
- Environ **+30 % CPU avec Java 8**
- Environ **+10 % CPU avec Java 9+**

Astuce :  
Utiliser un listener **non-SSL dédié à la communication inter-broker**, sur un réseau isolé réservé aux brokers, peut améliorer les performances.

---

## 4. Démo : Kafka Clients avec TLS

### Objectif
Chiffrer la communication **clients ↔ brokers** avec TLS.

### Étapes principales
1. Configuration des **keystores** et **truststores**
2. Ajout d’un listener `SSL` sur un port dédié (ex: 9191)
3. Exposition du port hors du réseau Docker
4. Déploiement progressif (rolling update) de tous les brokers

### Points importants
- `KEYSTORE_PASSWORD` : protège le keystore
- `KEY_PASSWORD` : protège la clé privée
- `SSL_CLIENT_AUTH=false` (authentification client désactivée pour l’instant)

---

## 5. Migration des producteurs et consommateurs vers TLS

### Changements nécessaires
- Utiliser les ports TLS (9191, 9192, 9193)
- Fournir keystore et truststore
- Ajouter : `security.protocol=SSL`


### Avantage clé
Les clients peuvent être **migrés indépendamment**, sans interruption de service (pas de “big bang”).

---

## 6. Démo : TLS inter-broker

### Objectif
Chiffrer la communication **broker ↔ broker**.

### Configuration
`security.inter.broker.protocol=SSL`

Une fois tous les clients migrés :
- Suppression du listener `PLAINTEXT`
- Redéploiement progressif des brokers

Résultat :  
✅ **Aucune interruption de service**  
✅ Réplication et clients fonctionnels

---

## 7. Limites de TLS et besoin du chiffrement de bout en bout

TLS :
- Chiffre le **canal de communication**
- ❌ Les données sont **décryptées sur le broker**

Risques restants :
- Un attaquant peut se faire passer pour un client
- Les données sont stockées en clair sur le disque du broker

Solutions complémentaires :
- Disques chiffrés
- **Chiffrement de bout en bout (E2E)**

---

## 8. Chiffrement de bout en bout (End-to-End Encryption)

### Principe
- Le **producteur chiffre les données**
- Le **consommateur les déchiffre**
- Les brokers ne voient **jamais les données en clair**

### Implémentation
- Basée sur les **serializers / deserializers**
- Algorithmes :
    - **Clé symétrique** → chiffrement des messages (rapide)
    - **Clé asymétrique (RSA)** → chiffrement des clés symétriques

### Défis
- Distribution des clés (Diffie-Hellman, API, etc.)
- Rotation des clés
- Granularité (cluster, topic, message, champ)
- Conformité légale (GDPR, CCPA)

---

## 9. Démo : chiffrement de bout en bout

### Étapes
1. Génération d’une paire de clés RSA avec OpenSSL
2. Ajout de la librairie : kafka-end-to-end-encryption (v1.0.1)
3. Configuration :
- `EncryptingSerializer` côté producteur
- `DecryptingDeserializer` côté consommateur

### Résultat
- Producteurs et consommateurs fonctionnent normalement
- Les logs Kafka montrent des **données illisibles**
- Seuls les consommateurs autorisés peuvent déchiffrer les messages

---

### Commande
    - `openssl genrsa -out keypair.pem 2048`
    - `openssl pkcs8 -topk8 -nocrypt -in keypair.pem -outform def -out private.key` 
    - `openssl rsa -in keypair.pem -outfrom der -pubout -out public.key`

## 10. Conclusion

À la fin de ce module :
- Toutes les communications Kafka sont chiffrées via TLS
- Les données peuvent être protégées **de bout en bout**
- La migration est possible **sans downtime**
- Kafka devient compatible avec des exigences élevées de sécurité et de conformité

➡️ Les prochains modules aborderont **l’authentification et l’autorisation** pour empêcher tout accès non autorisé aux données.

