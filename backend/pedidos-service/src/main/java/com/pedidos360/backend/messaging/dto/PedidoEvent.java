package com.pedidos360.backend.messaging.dto;

import java.time.Instant;

/** Mensaje que viaja por RabbitMQ cuando cambia el estado de un pedido. */
public record PedidoEvent(
        Long pedidoId,
        String cliente,
        String estado,
        Double total,
        boolean simularFallo,   // solo para demostrar NACK/DLQ en la defensa
        Instant fecha) {
}
