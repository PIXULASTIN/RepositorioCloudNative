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

    public Producto actualizar(Long id, Producto productoActualizado) {
        Producto producto = obtener(id);
        producto.setNombre(productoActualizado.getNombre());
        producto.setSku(productoActualizado.getSku());
        producto.setPrecio(productoActualizado.getPrecio());
        producto.setStockDisponible(productoActualizado.getStockDisponible());
        return productoRepository.save(producto);
    }

    public Producto descontarStock(Long id, int cantidad) {
        Producto producto = obtener(id);
        if (producto.getStockDisponible() < cantidad) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }
        producto.setStockDisponible(producto.getStockDisponible() - cantidad);
        return productoRepository.save(producto);
    }
}