package com.pedidos360.backend.messaging.kafka.service;

import com.pedidos360.backend.messaging.kafka.dto.StockKafkaEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockAlertaServiceTest {

    private final StockAlertaService service = new StockAlertaService(5);

    private StockKafkaEvent evento(int stock) {
        return new StockKafkaEvent("e1", 1L, "SKU-1", "Teclado", 10, stock, "DESCUENTO_PEDIDO", Instant.now());
    }

    @Test
    void stockBajoElUmbralGeneraAlerta() {
        assertTrue(service.evaluar(evento(3)));
        assertTrue(service.evaluar(evento(5)));
    }

    @Test
    void stockSobreElUmbralNoGeneraAlerta() {
        assertFalse(service.evaluar(evento(6)));
    }
}
