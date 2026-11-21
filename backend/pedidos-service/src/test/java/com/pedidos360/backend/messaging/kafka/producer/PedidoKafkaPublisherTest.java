package com.pedidos360.backend.messaging.kafka.producer;

import com.pedidos360.backend.messaging.kafka.config.KafkaAppProperties;
import com.pedidos360.backend.messaging.kafka.dto.PedidoKafkaEvent;
import com.pedidos360.backend.model.ItemPedido;
import com.pedidos360.backend.model.Pedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, PedidoKafkaEvent> kafkaTemplate;

    private PedidoKafkaPublisher publisher() {
        KafkaAppProperties props = new KafkaAppProperties(
                "localhost:9092", false,
                new KafkaAppProperties.Topics(
                        new KafkaAppProperties.Topic("pedidos.eventos", 3, 3, 604800000L, "delete", 2),
                        new KafkaAppProperties.Topic("catalogo.stock", 3, 3, 0L, "compact", 2)),
                new KafkaAppProperties.Producer("all", 3, true, 5, 15000, 30000));
        return new PedidoKafkaPublisher(kafkaTemplate, props);
    }

    private Pedido pedido() {
        List<ItemPedido> items = new ArrayList<>();
        items.add(new ItemPedido(1L, "Teclado", 2, 45000.0));
        return Pedido.builder().id(5L).cliente("Ana").creadoPor("ana@x.cl").estado("ACEPTADO").items(items).build();
    }

    @Test
    void publicarCambioEstado_enviaAlTopicoConKeyPedidoId() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoKafkaEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher().publicarCambioEstado(pedido(), "CREADO");

        ArgumentCaptor<PedidoKafkaEvent> captor = ArgumentCaptor.forClass(PedidoKafkaEvent.class);
        verify(kafkaTemplate).send(eq("pedidos.eventos"), eq("5"), captor.capture());

        PedidoKafkaEvent evento = captor.getValue();
        assertEquals(5L, evento.pedidoId());
        assertEquals("CREADO", evento.estadoAnterior());
        assertEquals("ACEPTADO", evento.estado());
        assertEquals(90000.0, evento.total());
        assertEquals(1, evento.items().size());
        assertNotNull(evento.eventId());
    }

    @Test
    void publicarCambioEstado_siKafkaFallaNoPropagaLaExcepcion() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoKafkaEvent.class)))
                .thenThrow(new IllegalStateException("broker caido"));

        assertDoesNotThrow(() -> publisher().publicarCambioEstado(pedido(), "CREADO"));
    }
}
