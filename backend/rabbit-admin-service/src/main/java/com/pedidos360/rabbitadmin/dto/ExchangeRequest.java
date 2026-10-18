package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.*;

public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "El nombre solo admite letras, numeros, '.', '_' y '-'")
        String name,

        @NotBlank(message = "El tipo de exchange es obligatorio")
        @Pattern(regexp = "^(direct|topic|fanout|headers)$", message = "type debe ser direct, topic, fanout o headers")
        String type,

        Boolean durable) {
}
