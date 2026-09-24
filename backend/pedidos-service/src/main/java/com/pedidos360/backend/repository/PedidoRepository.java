package com.pedidos360.backend.repository;

import com.pedidos360.backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByCreadoPor(String creadoPor);
}
