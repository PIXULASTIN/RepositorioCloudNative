package com.pedidos360.catalogo.config;

import com.pedidos360.catalogo.model.Producto;
import com.pedidos360.catalogo.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository productoRepository;

    public DataInitializer(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public void run(String... args) {
        if (productoRepository.count() == 0) {
            Producto p1 = new Producto();
            p1.setSku("PROD-001");
            p1.setNombre("Notebook ASUS1500 RF");
            p1.setPrecio(850000.0);
            p1.setStockDisponible(15);
            productoRepository.save(p1);

            Producto p2 = new Producto();
            p2.setSku("PROD-002");
            p2.setNombre("Teclado Mecánico RGB");
            p2.setPrecio(45000.0);
            p2.setStockDisponible(30);
            productoRepository.save(p2);
        }
    }
}