package com.pedidos360.catalogo.messaging.kafka.producer;

import com.pedidos360.catalogo.messaging.kafka.config.KafkaAppProperties;
import com.pedidos360.catalogo.messaging.kafka.dto.StockKafkaEvent;
import com.pedidos360.catalogo.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publica en catalogo.stock cada cambio de stock. Un fallo de Kafka se registra en el log
 * y nunca interrumpe la operacion de catalogo.
 */
@Component
public class StockKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockKafkaPublisher.class);

    private final KafkaTemplate<String, StockKafkaEvent> kafkaTemplate;
    private final KafkaAppProperties props;

    public StockKafkaPublisher(KafkaTemplate<String, StockKafkaEvent> kafkaTemplate, KafkaAppProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    /** @param stockAnterior stock antes del cambio (null si el producto es nuevo). */
    public void publicarCambioStock(Producto producto, Integer stockAnterior, String motivo) {
        StockKafkaEvent evento = StockKafkaEvent.desde(producto, stockAnterior, motivo);
        String topico = props.topics().stock().name();
        String key = String.valueOf(evento.productoId());
        try {
            kafkaTemplate.send(topico, key, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("[KAFKA-PRODUCER] Fallo al publicar producto={} en {}: {}",
                            evento.productoId(), topico, error.getMessage());
                } else {
                    registrarExito(evento, topico, resultado);
                }
            });
        } catch (RuntimeException ex) {
            log.error("[KAFKA-PRODUCER] No se pudo publicar el stock del producto {}: {}",
                    evento.productoId(), ex.getMessage());
        }
    }

    private void registrarExito(StockKafkaEvent evento, String topico, SendResult<String, StockKafkaEvent> r) {
        if (r != null && r.getRecordMetadata() != null) {
            log.info("[KAFKA-PRODUCER] producto={} stock={} motivo={} -> topico={} particion={} offset={}",
                    evento.productoId(), evento.stockActual(), evento.motivo(), topico,
                    r.getRecordMetadata().partition(), r.getRecordMetadata().offset());
        } else {
            log.info("[KAFKA-PRODUCER] producto={} stock={} motivo={} -> topico={}",
                    evento.productoId(), evento.stockActual(), evento.motivo(), topico);
        }
    }
}
