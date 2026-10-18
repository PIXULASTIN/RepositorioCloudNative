package com.pedidos360.backend.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbit")
public record MensajeriaProperties(
        Exchanges exchanges,
        Queues queues,
        RoutingKeys routingKeys,
        String dlqSuffix) {

    public record Exchanges(String direct, String topic, String dlx) {}

    public record Queues(String aceptados, String despachos, String notificaciones) {}

    public record RoutingKeys(String aceptado, String despachado, String notificaciones) {}

    /** Nombre de la DLQ de una cola: "<cola>.dlq". */
    public String dlq(String cola) {
        return cola + dlqSuffix;
    }
}
