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
public class PedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoConsumer.class);

    private final ConfirmacionMensajes confirmacion;

    public PedidoConsumer(ConfirmacionMensajes confirmacion) {
        this.confirmacion = confirmacion;
    }

    @RabbitListener(queues = "${app.rabbit.queues.aceptados}")
    public void onPedidoAceptado(PedidoEvent evento, Message message, Channel channel) throws IOException {
        try {
            validar(evento);
            log.info("[CONSUMER aceptados] Pedido {} aceptado ({}): se inicia la preparacion", evento.pedidoId(), evento.cliente());
            confirmacion.ack(message, channel);
        } catch (Exception e) {
            confirmacion.fallo(message, channel, e);
        }
    }

    @RabbitListener(queues = "${app.rabbit.queues.despachos}")
    public void onPedidoDespachado(PedidoEvent evento, Message message, Channel channel) throws IOException {
        try {
            validar(evento);
            log.info("[CONSUMER despachos] Pedido {} despachado ({}): se registra el envio", evento.pedidoId(), evento.cliente());
            confirmacion.ack(message, channel);
        } catch (Exception e) {
            confirmacion.fallo(message, channel, e);
        }
    }

    private void validar(PedidoEvent evento) {
        if (evento == null || evento.pedidoId() == null || evento.pedidoId() < 0) {
            throw new MensajeInvalidoException("Evento de pedido invalido (pedidoId ausente o negativo)");
        }
        if (evento.simularFallo()) {
            throw new IllegalStateException("Fallo simulado en el consumer");
        }
    }
}
