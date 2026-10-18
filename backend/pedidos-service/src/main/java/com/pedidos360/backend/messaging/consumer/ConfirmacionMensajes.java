package com.pedidos360.backend.messaging.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ConfirmacionMensajes {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionMensajes.class);

    public void ack(Message message, Channel channel) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    public void fallo(Message message, Channel channel, Exception error) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String cola = message.getMessageProperties().getConsumerQueue();
        boolean permanente = error instanceof MensajeInvalidoException;
        boolean yaReintentado = message.getMessageProperties().isRedelivered();

        if (permanente || yaReintentado) {
            log.error("[DLQ] Mensaje de {} NO procesado ({}). Se envia a la DLQ. Motivo: {} | cuerpo={}",
                    cola, permanente ? "error permanente" : "reintento agotado", error.getMessage(),
                    new String(message.getBody()));
            channel.basicNack(tag, false, false);
        } else {
            log.warn("[RETRY] Fallo procesando mensaje de {}: {}. Se reintenta una vez.", cola, error.getMessage());
            channel.basicNack(tag, false, true);  
        }
    }
}
