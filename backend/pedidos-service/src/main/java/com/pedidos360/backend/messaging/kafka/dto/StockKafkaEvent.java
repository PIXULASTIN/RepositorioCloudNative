package com.pedidos360.backend.messaging.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Evento del topico "catalogo.stock" (lo publica catalogo-service cuando cambia el stock de un producto).
 * Key del mensaje: productoId. El topico esta compactado: queda el ultimo evento de cada producto.
 */
public record StockKafkaEvent(
        @NotBlank String eventId,
        @NotNull Long productoId,
        String sku,
        @NotBlank String nombre,
        Integer stockAnterior,
        @NotNull @PositiveOrZero Integer stockActual,
        @NotBlank String motivo,
        @NotNull Instant fecha) {
}
