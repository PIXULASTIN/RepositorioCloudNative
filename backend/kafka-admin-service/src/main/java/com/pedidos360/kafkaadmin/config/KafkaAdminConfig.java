package com.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/** Cliente administrativo de Kafka (AdminClient) conectado a los brokers del cluster. */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(
            @Value("${app.kafka.bootstrap-servers:localhost:9092,localhost:9093,localhost:9094}") String bootstrap,
            @Value("${app.kafka.operation-timeout-seconds:15}") int timeoutSeconds) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        cfg.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutSeconds * 1000);
        cfg.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, timeoutSeconds * 1000);
        return Admin.create(cfg);
    }
}
