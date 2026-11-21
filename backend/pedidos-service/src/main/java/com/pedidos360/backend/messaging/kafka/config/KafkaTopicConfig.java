package com.pedidos360.backend.messaging.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

/**
 * Declara los topicos con sus particiones, replicas, retencion y cleanup policy.
 * KafkaAdmin los crea al arrancar si no existen (si ya existen, no los modifica).
 * Se desactiva con app.kafka.create-topics=false (por ejemplo en tests).
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.create-topics", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaAppProperties props) {
        KafkaAdmin admin = new KafkaAdmin(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers()));
        admin.setFatalIfBrokerNotAvailable(false);   // si Kafka esta caido, el servicio igual arranca
        admin.setAutoCreate(true);
        return admin;
    }

    @Bean
    public NewTopic pedidosEventosTopic(KafkaAppProperties props) {
        return construir(props.topics().pedidos());
    }

    @Bean
    public NewTopic catalogoStockTopic(KafkaAppProperties props) {
        return construir(props.topics().stock());
    }

    private NewTopic construir(KafkaAppProperties.Topic t) {
        TopicBuilder builder = TopicBuilder.name(t.name())
                .partitions(t.partitions())
                .replicas(t.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()));
        if (t.retentionMs() > 0) {
            builder.config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retentionMs()));
        }
        return builder.build();
    }
}
