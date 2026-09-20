package com.pedidos360.backend.repository;

import com.pedidos360.backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    // Spring Data genera la implementacion automaticamente.
    // Puedes agregar consultas personalizadas aqui, ej:
    // List<Pedido> findByEstado(String estado);
}
