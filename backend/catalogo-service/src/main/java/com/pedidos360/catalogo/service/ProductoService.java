package com.pedidos360.catalogo.service;

import com.pedidos360.catalogo.model.Producto;
import com.pedidos360.catalogo.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Producto crear(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
    }

    // Ejemplo de regla de negocio simple: descontar stock al confirmar un pedido.
    // Esto es un ejemplo de por que la logica de negocio va en el Service y no
    // directo en el Controller ni en el Repository.
    public Producto descontarStock(Long id, int cantidad) {
        Producto producto = obtener(id);
        if (producto.getStockDisponible() < cantidad) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }
        producto.setStockDisponible(producto.getStockDisponible() - cantidad);
        return productoRepository.save(producto);
    }
}
