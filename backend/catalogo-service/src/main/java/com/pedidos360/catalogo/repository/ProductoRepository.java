package com.pedidos360.catalogo.repository;

import com.pedidos360.catalogo.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Metodo personalizado: Spring Data lo implementa solo, basado en el nombre.
    // Traduce automaticamente a: SELECT * FROM productos WHERE sku = ?
    Optional<Producto> findBySku(String sku);
}
