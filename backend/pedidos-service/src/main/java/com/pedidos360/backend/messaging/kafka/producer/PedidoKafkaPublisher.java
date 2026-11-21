package com.pedidos360.backend.messaging.kafka.producer;

import com.pedidos360.backend.messaging.kafka.config.KafkaAppProperties;
import com.pedidos360.backend.messaging.kafka.dto.PedidoKafkaEvent;
import com.pedidos360.backend.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Productor Kafka de eventos de pedidos. Complementa (no reemplaza) a PedidoEventPublisher de RabbitMQ.
 * Un fallo de Kafka se registra en el log y NUNCA rompe el flujo del pedido ni el envio a RabbitMQ.
 */
@Component
public class PedidoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, PedidoKafkaEvent> kafkaTemplate;
    private final KafkaAppProperties props;

    public PedidoKafkaPublisher(KafkaTemplate<String, PedidoKafkaEvent> kafkaTemplate, KafkaAppProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    /** Publica el cambio de estado una vez confirmada la transaccion de base de datos. */
    public void publicarCambioEstado(Pedido pedido, String estadoAnterior) {
        PedidoKafkaEvent evento = PedidoKafkaEvent.desde(pedido, estadoAnterior);
        despuesDelCommit(() -> enviar(evento));
    }

    /** Evento de prueba para la demo (endpoint /api/admin/kafka/prueba). */
    public void publicarPrueba(Long pedidoId, String estado) {
        PedidoKafkaEvent evento = new PedidoKafkaEvent(UUID.randomUUID().toString(), pedidoId, "prueba",
                null, estado, 0.0, List.of(), Instant.now());
        enviar(evento);
    }

    private void enviar(PedidoKafkaEvent evento) {
        String topico = props.topics().pedidos().name();
        String key = String.valueOf(evento.pedidoId());
        try {
            kafkaTemplate.send(topico, key, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("[KAFKA-PRODUCER] Fallo al publicar pedido={} en {}: {}",
                            evento.pedidoId(), topico, error.getMessage());
                } else {
                    registrarExito(evento, topico, resultado);
                }
            });
        } catch (RuntimeException ex) {
            log.error("[KAFKA-PRODUCER] No se pudo publicar el evento del pedido {}: {}",
                    evento.pedidoId(), ex.getMessage());
        }
    }

    private void registrarExito(PedidoKafkaEvent evento, String topico, SendResult<String, PedidoKafkaEvent> r) {
        if (r != null && r.getRecordMetadata() != null) {
            log.info("[KAFKA-PRODUCER] pedido={} estado={} -> topico={} particion={} offset={}",
                    evento.pedidoId(), evento.estado(), topico,
                    r.getRecordMetadata().partition(), r.getRecordMetadata().offset());
        } else {
            log.info("[KAFKA-PRODUCER] pedido={} estado={} -> topico={}", evento.pedidoId(), evento.estado(), topico);
        }
    }

    private void despuesDelCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
