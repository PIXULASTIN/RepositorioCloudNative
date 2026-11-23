package com.pedidos360.catalogo.service;

import com.pedidos360.catalogo.messaging.kafka.producer.StockKafkaPublisher;
import com.pedidos360.catalogo.model.Producto;
import com.pedidos360.catalogo.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final StockKafkaPublisher kafkaPublisher;   // EP5: eventos de stock a Kafka

    public ProductoService(ProductoRepository productoRepository, StockKafkaPublisher kafkaPublisher) {
        this.productoRepository = productoRepository;
        this.kafkaPublisher = kafkaPublisher;
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Producto crear(Producto producto) {
        Producto guardado = productoRepository.save(producto);
        kafkaPublisher.publicarCambioStock(guardado, null, "CREACION");
        return guardado;
    }

    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + id));
    }

    public Producto actualizar(Long id, Producto productoActualizado) {
        Producto producto = obtener(id);
        Integer stockAnterior = producto.getStockDisponible();
        producto.setNombre(productoActualizado.getNombre());
        producto.setSku(productoActualizado.getSku());
        producto.setPrecio(productoActualizado.getPrecio());
        producto.setStockDisponible(productoActualizado.getStockDisponible());
        Producto guardado = productoRepository.save(producto);
        if (!Objects.equals(stockAnterior, guardado.getStockDisponible())) {
            kafkaPublisher.publicarCambioStock(guardado, stockAnterior, "AJUSTE_MANUAL");
        }
        return guardado;
    }

    public Producto descontarStock(Long id, int cantidad) {
        Producto producto = obtener(id);
        if (producto.getStockDisponible() < cantidad) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }
        Integer stockAnterior = producto.getStockDisponible();
        producto.setStockDisponible(stockAnterior - cantidad);
        Producto guardado = productoRepository.save(producto);
        kafkaPublisher.publicarCambioStock(guardado, stockAnterior, "DESCUENTO_PEDIDO");
        return guardado;
    }
}