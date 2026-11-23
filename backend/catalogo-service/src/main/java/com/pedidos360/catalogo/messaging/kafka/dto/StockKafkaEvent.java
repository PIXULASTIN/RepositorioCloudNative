package com.pedidos360.catalogo.messaging.kafka.dto;

import com.pedidos360.catalogo.model.Producto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento del topico "catalogo.stock": cambio de stock de un producto.
 * Key del mensaje: productoId. El topico esta compactado (queda el ultimo evento por producto).
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

    public static StockKafkaEvent desde(Producto p, Integer stockAnterior, String motivo) {
        return new StockKafkaEvent(UUID.randomUUID().toString(), p.getId(), p.getSku(), p.getNombre(),
                stockAnterior, p.getStockDisponible(), motivo, Instant.now());
    }
}
