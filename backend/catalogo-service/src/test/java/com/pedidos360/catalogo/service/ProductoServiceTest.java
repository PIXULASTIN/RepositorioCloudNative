package com.pedidos360.catalogo.service;

import com.pedidos360.catalogo.messaging.kafka.producer.StockKafkaPublisher;
import com.pedidos360.catalogo.model.Producto;
import com.pedidos360.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private StockKafkaPublisher kafkaPublisher;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void listar_debeRetornarTodosLosProductos() {
        Producto p1 = Producto.builder().id(1L).sku("PROD-001").nombre("Teclado").stockDisponible(10).precio(15990.0).build();
        when(productoRepository.findAll()).thenReturn(List.of(p1));

        List<Producto> resultado = productoService.listar();

        assertEquals(1, resultado.size());
    }

    @Test
    void descontarStock_debeRestarLaCantidad() {
        Producto producto = Producto.builder().id(1L).sku("PROD-001").nombre("Mouse").stockDisponible(5).precio(9990.0).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(producto)).thenReturn(producto);

        Producto resultado = productoService.descontarStock(1L, 2);

        assertEquals(3, resultado.getStockDisponible());
        verify(kafkaPublisher).publicarCambioStock(resultado, 5, "DESCUENTO_PEDIDO");   // EP5: evento a Kafka
    }

    @Test
    void descontarStock_debeFallar_siNoHayStockSuficiente() {
        Producto producto = Producto.builder().id(1L).sku("PROD-002").nombre("Monitor").stockDisponible(1).precio(99990.0).build();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThrows(IllegalStateException.class, () -> productoService.descontarStock(1L, 5));
        verify(kafkaPublisher, never()).publicarCambioStock(any(), any(), anyString());   // sin cambio no hay evento
    }
}
