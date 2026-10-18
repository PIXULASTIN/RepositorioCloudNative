package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.*;

public record QueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "El nombre solo admite letras, numeros, '.', '_' y '-'")
        String name,

        Boolean durable,

        @Min(value = 1, message = "messageTtlMs debe ser mayor a 0")
        Long messageTtlMs,

        @Min(value = 1, message = "maxLength debe ser mayor a 0")
        Long maxLength,

        @Size(max = 255, message = "deadLetterExchange demasiado largo")
        String deadLetterExchange,

        @Size(max = 255, message = "deadLetterRoutingKey demasiado largo")
        String deadLetterRoutingKey) {
}
