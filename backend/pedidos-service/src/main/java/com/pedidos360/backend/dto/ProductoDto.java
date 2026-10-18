package com.pedidos360.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(Long id, String sku, String nombre, Integer stockDisponible, Double precio) {
}
