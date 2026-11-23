package com.pedidos360.reporteria.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/** Evento del topico catalogo.stock (lo publica catalogo-service). Key = productoId. */
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
