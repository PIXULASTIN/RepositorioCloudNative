package com.pedidos360.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vista minima de un producto del catalogo-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(Long id, String sku, String nombre, Integer stockDisponible, Double precio) {
}
