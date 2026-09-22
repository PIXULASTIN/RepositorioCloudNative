package com.pedidos360.backend.controller;

import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listar(@AuthenticationPrincipal Jwt jwt) {
        String usuarioEmail = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(pedidoService.listar());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody Pedido pedido, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pedidoService.crear(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }
}