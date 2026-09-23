package com.pedidos360.catalogo.controller;

import com.pedidos360.catalogo.model.Producto;
import com.pedidos360.catalogo.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody Producto producto) {
        return ResponseEntity.ok(productoService.crear(producto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @PostMapping("/{id}/descontar-stock")
    public ResponseEntity<Producto> descontarStock(@PathVariable Long id, @RequestParam int cantidad) {
        return ResponseEntity.ok(productoService.descontarStock(id, cantidad));
    }
}
