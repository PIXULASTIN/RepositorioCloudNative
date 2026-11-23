package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Solicitud para crear un topico. "configs" admite retention.ms, cleanup.policy, min.insync.replicas, etc. */
public record TopicRequest(
        @NotBlank @Size(max = 249)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "solo letras, numeros, punto, guion y guion bajo")
        String name,
        @NotNull @Min(1) @Max(100) Integer partitions,
        @NotNull @Min(1) @Max(10) Integer replicationFactor,
        Map<String, String> configs) {
}
