package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nuevo total de particiones (Kafka solo permite aumentar). */
public record PartitionsRequest(@NotNull @Min(1) @Max(100) Integer partitions) {
}
