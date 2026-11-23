package com.pedidos360.backend.messaging.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.messaging.kafka.dto.StockKafkaEvent;
import com.pedidos360.backend.messaging.kafka.error.EventoInvalidoException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente Kafka (lado consumidor): deserializacion segura, consumer group, commit manual por registro y
 * manejo de errores (reintentos con backoff exponencial + Dead Letter Topic).
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConsumerFactory<String, StockKafkaEvent> stockConsumerFactory(KafkaAppProperties props) {
        KafkaAppProperties.Consumer c = props.consumer();

        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, c.groups().pedidosStock());
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, c.autoOffsetReset());
        cfg.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);          // commit lo hace Spring (AckMode.RECORD)
        cfg.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, c.maxPollRecords());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // ErrorHandlingDeserializer: un mensaje corrupto NO detiene al consumidor, se envia a la DLT.
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        cfg.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        cfg.put(JsonDeserializer.VALUE_DEFAULT_TYPE, StockKafkaEvent.class.getName());
        cfg.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        cfg.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pedidos360.backend.messaging.kafka.dto");

        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    /** Template para publicar en las DLT: acepta bytes (mensaje corrupto), texto u objetos (JSON). */
    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(KafkaAppProperties props, ObjectMapper objectMapper) {
        JsonSerializer<Object> json = new JsonSerializer<>(objectMapper);
        json.setAddTypeInfo(false);

        Map<Class<?>, Serializer<?>> delegados = new LinkedHashMap<>();   // Object va al final
        delegados.put(byte[].class, new ByteArraySerializer());
        delegados.put(String.class, new StringSerializer());
        delegados.put(Object.class, json);

        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        cfg.put(ProducerConfig.ACKS_CONFIG, "all");
        cfg.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, props.producer().maxBlockMs());

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, Object>(
                cfg, new StringSerializer(), new DelegatingByTypeSerializer(delegados, true)));
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dltKafkaTemplate,
                                                 KafkaAppProperties props) {
        // <topico>.DLT, misma particion que el mensaje original
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        KafkaAppProperties.Retry r = props.consumer().retry();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(r.maxRetries());
        backOff.setInitialInterval(r.initialIntervalMs());
        backOff.setMultiplier(r.multiplier());
        backOff.setMaxInterval(r.maxIntervalMs());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(EventoInvalidoException.class);   // datos invalidos: directo a la DLT
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("[KAFKA-RETRY] intento={} topico={} particion={} offset={} causa={}",
                        deliveryAttempt, record.topic(), record.partition(), record.offset(), causa(ex));
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("[KAFKA-DLT] enviado a {}.DLT topico={} particion={} offset={} causa={}",
                        record.topic(), record.topic(), record.partition(), record.offset(), causa(ex));
            }
        });
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockKafkaEvent> stockListenerContainerFactory(
            ConsumerFactory<String, StockKafkaEvent> stockConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            KafkaAppProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, StockKafkaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stockConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(props.consumer().concurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setAutoStartup(props.consumer().autoStartup());
        return factory;
    }

    private static String causa(Exception ex) {
        Throwable t = ex.getCause() != null ? ex.getCause() : ex;
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
