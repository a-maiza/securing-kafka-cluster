package com.pluralsight.kafka.security.authorization;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class SASLSCRAMKafkaStreams {

    private static final Logger log = LoggerFactory.getLogger(SASLSCRAMKafkaStreams.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams.app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker-1:9391,broker-2:9392,broker-3:9393");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Long().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");

        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/Users/bogdan/pluralsight/securing-kafka-cluster/m6/security/truststore/producer.truststore.jks"); // Replace with the absolute path on your machine
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "password");

        StreamsBuilder builder = new StreamsBuilder();

        KStream<Long, String> stream = builder.stream("auth-topic");

        stream
                .groupByKey()
                .count(Materialized.as("counts"))
                .toStream()
                .to("final-topic");


        Topology topology = builder.build();

        // Describe Topology in order to determine the existence of intermediate topics
        //log.info("Topology Description: " + topology.describe().toString());

        KafkaStreams streams = new KafkaStreams(topology, props);

        Thread haltedHook = new Thread(streams::close);
        Runtime.getRuntime().addShutdownHook(haltedHook);

        streams.start();
    }
}
