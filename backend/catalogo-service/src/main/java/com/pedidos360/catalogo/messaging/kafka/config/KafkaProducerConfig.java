package com.pedidos360.catalogo.messaging.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.catalogo.messaging.kafka.dto.StockKafkaEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/** Cliente Kafka (lado productor): brokers, serializers y propiedades de confiabilidad. */
@Configuration
@EnableConfigurationProperties(KafkaAppProperties.class)
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, StockKafkaEvent> stockProducerFactory(KafkaAppProperties props,
                                                                         ObjectMapper objectMapper) {
        KafkaAppProperties.Producer p = props.producer();

        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        cfg.put(ProducerConfig.ACKS_CONFIG, p.acks());
        cfg.put(ProducerConfig.RETRIES_CONFIG, p.retries());
        cfg.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, p.enableIdempotence());
        cfg.put(ProducerConfig.LINGER_MS_CONFIG, p.lingerMs());
        cfg.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, p.requestTimeoutMs());
        cfg.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, p.deliveryTimeoutMs());
        cfg.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, p.maxBlockMs());

        // JSON ISO-8601 y sin cabecera de tipo Java: cualquier servicio lo lee con su propio DTO.
        JsonSerializer<StockKafkaEvent> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(cfg, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, StockKafkaEvent> stockKafkaTemplate(
            ProducerFactory<String, StockKafkaEvent> stockProducerFactory) {
        return new KafkaTemplate<>(stockProducerFactory);
    }
}
