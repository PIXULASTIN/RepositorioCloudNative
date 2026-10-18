package com.pedidos360.backend.messaging.producer;

import com.pedidos360.backend.messaging.config.MensajeriaProperties;
import com.pedidos360.backend.messaging.dto.PedidoEvent;
import com.pedidos360.backend.model.EstadoPedido;
import com.pedidos360.backend.model.ItemPedido;
import com.pedidos360.backend.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Locale;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public void publicarCambioEstado(Pedido pedido) {
        PedidoEvent evento = new PedidoEvent(pedido.getId(), pedido.getCliente(), pedido.getEstado(),
                total(pedido), false, Instant.now());
        despuesDelCommit(() -> enviar(evento));
    }

    public void publicarPrueba(String exchangeTipo, String routingKey, boolean simularFallo) {
        PedidoEvent evento = new PedidoEvent(0L, "prueba", "PRUEBA", 0.0, simularFallo, Instant.now());
        String exchange = "direct".equalsIgnoreCase(exchangeTipo) ? props.exchanges().direct() : props.exchanges().topic();
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        log.info("[PRODUCER] Mensaje de prueba -> exchange={} rk={} simularFallo={}", exchange, routingKey, simularFallo);
    }

    private void enviar(PedidoEvent e) {
        try {
            String estado = e.estado().toUpperCase(Locale.ROOT);
            String rkTopic = "pedido." + estado.toLowerCase(Locale.ROOT);
            rabbitTemplate.convertAndSend(props.exchanges().topic(), rkTopic, e);
            log.info("[PRODUCER] pedido={} -> exchange={} rk={}", e.pedidoId(), props.exchanges().topic(), rkTopic);

            if (estado.equals(EstadoPedido.ACEPTADO.name())) {
                rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().aceptado(), e);
                log.info("[PRODUCER] pedido={} -> exchange={} rk={}", e.pedidoId(), props.exchanges().direct(), props.routingKeys().aceptado());
            } else if (estado.equals(EstadoPedido.DESPACHADO.name())) {
                rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().despachado(), e);
                log.info("[PRODUCER] pedido={} -> exchange={} rk={}", e.pedidoId(), props.exchanges().direct(), props.routingKeys().despachado());
            }
        } catch (AmqpException ex) {
            log.error("[PRODUCER] No se pudo publicar el evento del pedido {}: {}", e.pedidoId(), ex.getMessage());
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

    private Double total(Pedido p) {
        if (p.getItems() == null) {
            return 0.0;
        }
        return p.getItems().stream()
                .mapToDouble((ItemPedido i) -> i.getCantidad() * i.getPrecioUnitario())
                .sum();
    }
}
