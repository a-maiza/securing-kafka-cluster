docker compose down
docker compose up -d
docker exec -it broker-1 ls -l /etc/kafka/secrets
docker exec -it broker-1 cat /etc/kafka/secrets/key_creds
mkdir -p ./security/creds
echo -n "password" > ./security/creds/broker-1_keystore_creds
echo -n "password" > ./security/creds/broker-1_key_creds
echo -n "password" > ./security/creds/broker-1_truststore_creds

keytool -list -v -keystore ./security/truststore/broker-1.truststore.jks -storepass password | head -n 50
keytool -list -v -keystore ./security/keystore/broker-1.keystore.jks -storepass password | grep -E "Alias|Entry type|Certificate chain length"

docker exec -it broker-1 ls -l /etc/kafka/secrets
docker exec -it broker-1 keytool -list -keystore /etc/kafka/secrets/broker-1.truststore.jks -storepass password | head
docker exec -it broker-1 keytool -list -keystore /etc/kafka/secrets/broker-1.keystore.jks -storepass password | head


