package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.*;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Administracion de Kafka por REST. Solo rol ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    // ---- cluster
    @GetMapping("/cluster")
    public ResponseEntity<ClusterInfoResponse> cluster() {
        return ResponseEntity.ok(service.infoCluster());
    }

    // ---- topicos
    @GetMapping("/topics")
    public ResponseEntity<List<String>> listarTopicos() {
        return ResponseEntity.ok(service.listarTopicos());
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<TopicInfoResponse> describirTopico(@PathVariable String name) {
        return ResponseEntity.ok(service.describirTopico(name));
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> crearTopico(@Valid @RequestBody TopicRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTopico(req));
    }

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<TopicInfoResponse> actualizarConfig(@PathVariable String name,
                                                              @Valid @RequestBody ConfigUpdateRequest req) {
        return ResponseEntity.ok(service.actualizarConfig(name, req.configs()));
    }

    @PutMapping("/topics/{name}/partitions")
    public ResponseEntity<Map<String, Object>> aumentarParticiones(@PathVariable String name,
                                                                   @Valid @RequestBody PartitionsRequest req) {
        return ResponseEntity.ok(service.aumentarParticiones(name, req.partitions()));
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String name) {
        service.eliminarTopico(name);
        return ResponseEntity.noContent().build();
    }

    // ---- consumer groups
    @GetMapping("/consumer-groups")
    public ResponseEntity<List<GrupoResumen>> listarGrupos() {
        return ResponseEntity.ok(service.listarGrupos());
    }

    @GetMapping("/consumer-groups/{id}")
    public ResponseEntity<GrupoInfoResponse> describirGrupo(@PathVariable String id) {
        return ResponseEntity.ok(service.describirGrupo(id));
    }

    @GetMapping("/consumer-groups/{id}/lag")
    public ResponseEntity<GrupoLagResponse> lagGrupo(@PathVariable String id) {
        return ResponseEntity.ok(service.lagGrupo(id));
    }

    @DeleteMapping("/consumer-groups/{id}")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable String id) {
        service.eliminarGrupo(id);
        return ResponseEntity.noContent().build();
    }
}
