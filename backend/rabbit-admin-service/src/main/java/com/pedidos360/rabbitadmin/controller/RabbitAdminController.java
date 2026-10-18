package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.*;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rabbit")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    public ResponseEntity<QueueInfoResponse> crearCola(@Valid @RequestBody QueueRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCola(req));
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<QueueInfoResponse> infoCola(@PathVariable String name) {
        return ResponseEntity.ok(service.infoCola(name));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest req) {
        service.crearExchange(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("name", req.name(), "type", req.type()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> crearBinding(@Valid @RequestBody BindingRequest req) {
        service.crearBinding(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest req) {
        service.eliminarBinding(req);
        return ResponseEntity.noContent().build();
    }
}
