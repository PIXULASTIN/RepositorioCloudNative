package com.pedidos360.reporteria.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;

/** Evento de topico pedidos.eventos (lo publica pedidos-service). Key = pedidoId. */
public record PedidoKafkaEvent(
        @NotBlank String eventId,
        @NotNull Long pedidoId,
        String cliente,
        String estadoAnterior,
        @NotBlank String estado,
        @NotNull @PositiveOrZero Double total,
        List<Item> items,
        @NotNull Instant fecha) {

    public record Item(Long productoId, String nombreProducto, Integer cantidad, Double precioUnitario) {
    }
}
