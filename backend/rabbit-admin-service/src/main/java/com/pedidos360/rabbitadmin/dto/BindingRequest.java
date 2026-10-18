package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BindingRequest(
        @NotBlank(message = "La cola es obligatoria")
        String queue,

        @NotBlank(message = "El exchange es obligatorio")
        String exchange,

        @Size(max = 255, message = "routingKey demasiado larga")
        String routingKey) {
}
