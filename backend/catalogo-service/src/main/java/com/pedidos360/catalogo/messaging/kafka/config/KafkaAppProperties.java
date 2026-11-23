package com.pedidos360.catalogo.messaging.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades Kafka del servicio (prefijo app.kafka). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAppProperties(
        String bootstrapServers,
        boolean createTopics,
        Topics topics,
        Producer producer) {

    public record Topics(Topic stock) {
    }

    public record Topic(
            String name,
            int partitions,
            int replicas,
            long retentionMs,
            String cleanupPolicy,
            int minInsyncReplicas) {
    }

    public record Producer(
            String acks,
            int retries,
            boolean enableIdempotence,
            int lingerMs,
            int requestTimeoutMs,
            int deliveryTimeoutMs,
            int maxBlockMs) {
    }
}
