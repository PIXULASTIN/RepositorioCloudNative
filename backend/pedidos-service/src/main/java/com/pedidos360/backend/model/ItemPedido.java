package com.pedidos360.backend.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Linea de un pedido. Nombre y precio se copian del catalogo al crear el pedido. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {
    private Long productoId;
    private String nombreProducto;
    private Integer cantidad;
    private Double precioUnitario;
}
