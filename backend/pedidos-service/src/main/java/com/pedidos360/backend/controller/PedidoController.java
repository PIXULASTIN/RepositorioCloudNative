package com.pedidos360.backend.controller;

import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class PedidoController {

    public record CambioEstado(String estado) {}

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listar(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return ResponseEntity.ok(pedidoService.listar(usuario(jwt), esStaff(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return ResponseEntity.ok(pedidoService.obtener(id, usuario(jwt), esStaff(auth)));
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody Pedido pedido, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        // Un CLIENTE siempre compra a su nombre; ADMIN/OPERADOR pueden indicar el cliente.
        String cliente = (esStaff(auth) && pedido.getCliente() != null && !pedido.getCliente().isBlank())
                ? pedido.getCliente()
                : nombre(jwt);
        return ResponseEntity.ok(pedidoService.crear(pedido, usuario(jwt), cliente));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Pedido> cambiarEstado(@PathVariable Long id, @RequestBody CambioEstado body) {
        // El acceso (OPERADOR/ADMIN) se controla en SecurityConfig
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, body.estado()));
    }

    // ---- helpers ----

    private boolean esStaff(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_OPERADOR"));
    }

    private String usuario(Jwt jwt) {
        for (String claim : List.of("preferred_username", "upn", "unique_name", "email")) {
            String v = jwt.getClaimAsString(claim);
            if (v != null && !v.isBlank()) {
                return v.toLowerCase();
            }
        }
        return jwt.getSubject();
    }

    private String nombre(Jwt jwt) {
        String n = jwt.getClaimAsString("name");
        return (n != null && !n.isBlank()) ? n : usuario(jwt);
    }
}
