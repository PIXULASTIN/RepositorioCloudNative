package com.pedidos360.backend.controller;

import com.pedidos360.backend.messaging.producer.PedidoEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/mensajeria")
public class MensajeriaController {

    public record MensajePrueba(String exchange, String routingKey, boolean simularFallo) {}

    private static final Set<String> EXCHANGES = Set.of("direct", "topic");

    private final PedidoEventPublisher publisher;

    public MensajeriaController(PedidoEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/prueba")
    public ResponseEntity<Map<String, String>> publicarPrueba(@RequestBody MensajePrueba body) {
        if (body.exchange() == null || !EXCHANGES.contains(body.exchange().toLowerCase())) {
            throw new IllegalArgumentException("exchange debe ser 'direct' o 'topic'");
        }
        if (body.routingKey() == null || body.routingKey().isBlank()) {
            throw new IllegalArgumentException("routingKey es obligatoria");
        }
        publisher.publicarPrueba(body.exchange(), body.routingKey().trim(), body.simularFallo());
        return ResponseEntity.accepted().body(Map.of("mensaje", "Mensaje publicado"));
    }
}
