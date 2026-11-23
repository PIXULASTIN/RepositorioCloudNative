package com.pedidos360.backend.messaging.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades Kafka del proyecto (prefijo app.kafka). Un solo lugar para toda la configuracion. */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAppProperties(
        String bootstrapServers,
        boolean createTopics,
        Topics topics,
        Producer producer,
        Consumer consumer) {

    public record Topics(Topic pedidos, Topic stock) {
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

    public record Consumer(
            boolean autoStartup,
            String autoOffsetReset,
            int concurrency,
            int maxPollRecords,
            Groups groups,
            Retry retry) {
    }

    public record Groups(String pedidosStock) {
    }

    public record Retry(
            long initialIntervalMs,
            double multiplier,
            long maxIntervalMs,
            int maxRetries) {
    }
}
