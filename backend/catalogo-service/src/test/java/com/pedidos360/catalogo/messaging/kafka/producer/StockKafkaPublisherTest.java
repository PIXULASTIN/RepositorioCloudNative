package com.pedidos360.catalogo.messaging.kafka.producer;

import com.pedidos360.catalogo.messaging.kafka.config.KafkaAppProperties;
import com.pedidos360.catalogo.messaging.kafka.dto.StockKafkaEvent;
import com.pedidos360.catalogo.model.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, StockKafkaEvent> kafkaTemplate;

    private StockKafkaPublisher publisher() {
        KafkaAppProperties props = new KafkaAppProperties(
                "localhost:9092", false,
                new KafkaAppProperties.Topics(
                        new KafkaAppProperties.Topic("catalogo.stock", 3, 3, 0L, "compact", 2)),
                new KafkaAppProperties.Producer("all", 3, true, 5, 15000, 30000, 3000));
        return new StockKafkaPublisher(kafkaTemplate, props);
    }

    private Producto producto() {
        return Producto.builder().id(7L).sku("SKU-7").nombre("Mouse").stockDisponible(3).precio(9990.0).build();
    }

    @Test
    void publicarCambioStock_enviaAlTopicoConKeyProductoId() {
        when(kafkaTemplate.send(anyString(), anyString(), any(StockKafkaEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher().publicarCambioStock(producto(), 5, "DESCUENTO_PEDIDO");

        ArgumentCaptor<StockKafkaEvent> captor = ArgumentCaptor.forClass(StockKafkaEvent.class);
        verify(kafkaTemplate).send(eq("catalogo.stock"), eq("7"), captor.capture());

        StockKafkaEvent evento = captor.getValue();
        assertEquals(7L, evento.productoId());
        assertEquals(5, evento.stockAnterior());
        assertEquals(3, evento.stockActual());
        assertEquals("DESCUENTO_PEDIDO", evento.motivo());
        assertNotNull(evento.eventId());
    }

    @Test
    void publicarCambioStock_siKafkaFallaNoPropagaLaExcepcion() {
        when(kafkaTemplate.send(anyString(), anyString(), any(StockKafkaEvent.class)))
                .thenThrow(new IllegalStateException("broker caido"));

        assertDoesNotThrow(() -> publisher().publicarCambioStock(producto(), 5, "AJUSTE_MANUAL"));
    }
}
