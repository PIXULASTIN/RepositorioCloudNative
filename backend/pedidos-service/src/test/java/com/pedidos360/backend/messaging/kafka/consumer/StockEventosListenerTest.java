package com.pedidos360.backend.messaging.kafka.consumer;

import com.pedidos360.backend.messaging.kafka.dto.StockKafkaEvent;
import com.pedidos360.backend.messaging.kafka.error.EventoInvalidoException;
import com.pedidos360.backend.messaging.kafka.service.StockAlertaService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockEventosListenerTest {

    @Mock
    private StockAlertaService alertaService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private StockEventosListener listener() {
        return new StockEventosListener(alertaService, validator);
    }

    @Test
    void eventoValidoSeDelegaAlServicioDeNegocio() {
        StockKafkaEvent evento = new StockKafkaEvent("e1", 1L, "SKU-1", "Teclado", 10, 4, "DESCUENTO_PEDIDO", Instant.now());

        listener().onStock(new ConsumerRecord<>("catalogo.stock", 0, 0L, "1", evento));

        verify(alertaService).evaluar(evento);
    }

    @Test
    void eventoInvalidoLanzaExcepcionNoReintentableYNoLlegaAlNegocio() {
        StockKafkaEvent invalido = new StockKafkaEvent("", null, null, "", null, -1, "", null);

        assertThrows(EventoInvalidoException.class,
                () -> listener().onStock(new ConsumerRecord<>("catalogo.stock", 0, 1L, "1", invalido)));

        verify(alertaService, never()).evaluar(any());
    }

    @Test
    void tombstoneSeIgnora() {
        listener().onStock(new ConsumerRecord<>("catalogo.stock", 0, 2L, "1", null));

        verify(alertaService, never()).evaluar(any());
    }
}
