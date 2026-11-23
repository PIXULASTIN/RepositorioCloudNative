package com.pedidos360.reporteria.messaging;

import com.pedidos360.reporteria.messaging.consumer.PedidoEventosListener;
import com.pedidos360.reporteria.messaging.dto.PedidoKafkaEvent;
import com.pedidos360.reporteria.messaging.error.EventoInvalidoException;
import com.pedidos360.reporteria.service.ReporteService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PedidoEventosListenerTest {

    @Mock
    private ReporteService reporteService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void eventoValidoSeDelegaAlServicioDeNegocio() {
        PedidoKafkaEvent evento = new PedidoKafkaEvent("e1", 5L, "Ana", "CREADO", "ACEPTADO", 100.0,
                List.of(), Instant.now());

        new PedidoEventosListener(reporteService, validator)
                .onPedido(new ConsumerRecord<>("pedidos.eventos", 0, 0L, "5", evento));

        verify(reporteService).registrarPedido(evento);
    }

    @Test
    void eventoInvalidoLanzaExcepcionNoReintentableYNoLlegaAlNegocio() {
        PedidoKafkaEvent invalido = new PedidoKafkaEvent("", null, null, null, "", -5.0, null, null);

        assertThrows(EventoInvalidoException.class,
                () -> new PedidoEventosListener(reporteService, validator)
                        .onPedido(new ConsumerRecord<>("pedidos.eventos", 0, 1L, "5", invalido)));

        verify(reporteService, never()).registrarPedido(any());
    }
}
