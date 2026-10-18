package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.ExchangeRequest;
import com.pedidos360.rabbitadmin.dto.QueueInfoResponse;
import com.pedidos360.rabbitadmin.dto.QueueRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @InjectMocks
    private RabbitAdminService service;

    @Test
    void crearCola_declaraColaConArgumentos() {
        when(amqpAdmin.getQueueInfo("demo.queue")).thenReturn(new QueueInformation("demo.queue", 0, 0));

        QueueInfoResponse resp = service.crearCola(
                new QueueRequest("demo.queue", true, 60000L, 100L, "demo.dlx", "demo.queue.dlq"));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertEquals("demo.queue", captor.getValue().getName());
        assertEquals("demo.dlx", captor.getValue().getArguments().get("x-dead-letter-exchange"));
        assertEquals(60000, captor.getValue().getArguments().get("x-message-ttl"));
        assertEquals("demo.queue", resp.name());
    }

    @Test
    void crearCola_rechazaNombreReservado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCola(new QueueRequest("amq.algo", null, null, null, null, null)));
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void crearCola_dlRoutingKeySinExchangeEsInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCola(new QueueRequest("demo", null, null, null, null, "rk")));
    }

    @Test
    void eliminarCola_noExisteLanza404() {
        when(amqpAdmin.getQueueInfo("nada")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> service.eliminarCola("nada"));
        verify(amqpAdmin, never()).deleteQueue(any());
    }

    @Test
    void crearExchange_topic() {
        service.crearExchange(new ExchangeRequest("demo.topic", "topic", true));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertEquals(ExchangeTypes.TOPIC, captor.getValue().getType());
    }

    @Test
    void eliminarExchange_noExisteLanza404() {
        when(amqpAdmin.deleteExchange("nada")).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> service.eliminarExchange("nada"));
    }

    @Test
    void crearBinding_declaraBindingColaExchange() {
        when(amqpAdmin.getQueueInfo("q")).thenReturn(new QueueInformation("q", 0, 0));

        service.crearBinding(new BindingRequest("q", "ex", "rk"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        assertEquals("q", captor.getValue().getDestination());
        assertEquals("ex", captor.getValue().getExchange());
        assertEquals("rk", captor.getValue().getRoutingKey());
    }
}
