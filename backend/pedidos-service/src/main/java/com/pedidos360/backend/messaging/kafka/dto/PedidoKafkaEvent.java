package com.pedidos360.backend.messaging.kafka.dto;

import com.pedidos360.backend.model.ItemPedido;
import com.pedidos360.backend.model.Pedido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Evento que viaja por el topico Kafka "pedidos.eventos" cuando cambia el estado de un pedido.
 * Key del mensaje: pedidoId (todos los eventos de un pedido caen en la misma particion y
 * conservan su orden).
 */
public record PedidoKafkaEvent(
        @NotBlank String eventId,
        @NotNull Long pedidoId,
        @NotBlank String cliente,
        String estadoAnterior,
        @NotBlank String estado,
        @NotNull @PositiveOrZero Double total,
        List<Item> items,
        @NotNull Instant fecha) {

    public record Item(Long productoId, String nombreProducto, Integer cantidad, Double precioUnitario) {
    }

    /** Construye el evento a partir de la entidad Pedido ya guardada. */
    public static PedidoKafkaEvent desde(Pedido pedido, String estadoAnterior) {
        List<ItemPedido> origen = pedido.getItems() == null ? List.of() : pedido.getItems();
        List<Item> items = origen.stream()
                .map(i -> new Item(i.getProductoId(), i.getNombreProducto(), i.getCantidad(), i.getPrecioUnitario()))
                .toList();
        double total = origen.stream()
                .mapToDouble(i -> i.getCantidad() * i.getPrecioUnitario())
                .sum();
        return new PedidoKafkaEvent(UUID.randomUUID().toString(), pedido.getId(), pedido.getCliente(),
                estadoAnterior, pedido.getEstado(), total, items, Instant.now());
    }
}
