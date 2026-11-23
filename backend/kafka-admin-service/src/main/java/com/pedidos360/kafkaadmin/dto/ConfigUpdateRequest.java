package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Configuraciones a modificar en un topico, por ejemplo {"retention.ms":"86400000"}. */
public record ConfigUpdateRequest(@NotEmpty Map<String, String> configs) {
}
