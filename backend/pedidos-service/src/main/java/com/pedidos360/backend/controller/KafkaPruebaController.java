package com.pedidos360.backend.controller;

import com.pedidos360.backend.messaging.kafka.producer.PedidoKafkaPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Publica un evento de prueba en Kafka (solo ADMIN, por la regla /api/admin/** de SecurityConfig). */
@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaPruebaController {

    public record PruebaKafka(Long pedidoId, String estado) {
    }

    private final PedidoKafkaPublisher publisher;

    public KafkaPruebaController(PedidoKafkaPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/prueba")
    public ResponseEntity<Map<String, String>> publicarPrueba(@RequestBody PruebaKafka body) {
        if (body.pedidoId() == null) {
            throw new IllegalArgumentException("pedidoId es obligatorio");
        }
        if (body.estado() == null || body.estado().isBlank()) {
            throw new IllegalArgumentException("estado es obligatorio");
        }
        publisher.publicarPrueba(body.pedidoId(), body.estado().trim().toUpperCase());
        return ResponseEntity.accepted().body(Map.of("mensaje", "Evento publicado en Kafka"));
    }
}
