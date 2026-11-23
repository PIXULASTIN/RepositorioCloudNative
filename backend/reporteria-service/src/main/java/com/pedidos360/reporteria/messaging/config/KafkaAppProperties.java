package com.pedidos360.reporteria.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades Kafka del servicio (prefijo app.kafka). */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAppProperties(
        String bootstrapServers,
        Topics topics,
        Consumer consumer) {

    public record Topics(String pedidos, String stock) {
    }

    public record Consumer(
            boolean autoStartup,
            String autoOffsetReset,
            int maxPollRecords,
            Groups groups,
            Concurrency concurrency,
            Retry retry) {
    }

    public record Groups(String pedidos, String stock) {
    }

    public record Concurrency(int pedidos, int stock) {
    }

    public record Retry(
            long initialIntervalMs,
            double multiplier,
            long maxIntervalMs,
            int maxRetries) {
    }
}
