package com.pedidos360.backend.messaging.consumer;

import com.pedidos360.backend.messaging.dto.PedidoEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConsumer.class);

    private final ConfirmacionMensajes confirmacion;

    public NotificacionConsumer(ConfirmacionMensajes confirmacion) {
        this.confirmacion = confirmacion;
    }

    @RabbitListener(queues = "${app.rabbit.queues.notificaciones}")
    public void onNotificacion(PedidoEvent evento, Message message, Channel channel) throws IOException {
        try {
            if (evento == null || evento.pedidoId() == null || evento.pedidoId() < 0) {
                throw new MensajeInvalidoException("Notificacion invalida (pedidoId ausente o negativo)");
            }
            if (evento.simularFallo()) {
                throw new IllegalStateException("Fallo simulado al notificar");
            }
            log.info("[CONSUMER notificaciones] Notificar a {}: su pedido {} ahora esta {}", evento.cliente(), evento.pedidoId(), evento.estado());
            confirmacion.ack(message, channel);
        } catch (Exception e) {
            confirmacion.fallo(message, channel, e);
        }
    }
}
