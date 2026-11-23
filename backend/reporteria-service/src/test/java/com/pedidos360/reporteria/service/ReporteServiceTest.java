package com.pedidos360.reporteria.service;

import com.pedidos360.reporteria.messaging.dto.PedidoKafkaEvent;
import com.pedidos360.reporteria.messaging.dto.StockKafkaEvent;
import com.pedidos360.reporteria.model.ResumenReporte;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteServiceTest {

    private final ReporteService service = new ReporteService(5);

    private PedidoKafkaEvent pedido(String id, String estado, double total, int cantidad) {
        return new PedidoKafkaEvent(id, 1L, "Ana", "CREADO", estado, total,
                List.of(new PedidoKafkaEvent.Item(10L, "Teclado", cantidad, 45000.0)), Instant.now());
    }

    private StockKafkaEvent stock(String id, long productoId, int stock, Instant fecha) {
        return new StockKafkaEvent(id, productoId, "SKU-" + productoId, "Producto " + productoId,
                null, stock, "AJUSTE_MANUAL", fecha);
    }

    @Test
    void pedidoAceptadoSumaIngresosYUnidadesVendidas() {
        service.registrarPedido(pedido("e1", "ACEPTADO", 90000, 2));
        service.registrarPedido(pedido("e2", "ACEPTADO", 45000, 1));

        ResumenReporte r = service.resumen();

        assertEquals(2L, r.pedidosPorEstado().get("ACEPTADO"));
        assertEquals(135000.0, r.ingresosAceptados());
        assertEquals("Teclado", r.productosMasVendidos().get(0).producto());
        assertEquals(3, r.productosMasVendidos().get(0).unidades());
        assertEquals(2, r.eventosPedidosProcesados());
    }

    @Test
    void pedidoNoAceptadoSoloCuentaPorEstado() {
        service.registrarPedido(pedido("e1", "DESPACHADO", 90000, 2));

        ResumenReporte r = service.resumen();

        assertEquals(1L, r.pedidosPorEstado().get("DESPACHADO"));
        assertEquals(0.0, r.ingresosAceptados());
        assertTrue(r.productosMasVendidos().isEmpty());
    }

    @Test
    void eventoDuplicadoSeIgnora() {
        assertTrue(service.registrarPedido(pedido("mismo", "ACEPTADO", 100, 1)));
        assertFalse(service.registrarPedido(pedido("mismo", "ACEPTADO", 100, 1)));

        ResumenReporte r = service.resumen();

        assertEquals(1, r.eventosPedidosProcesados());
        assertEquals(1, r.eventosDuplicadosIgnorados());
        assertEquals(100.0, r.ingresosAceptados());
    }

    @Test
    void stockConservaElEventoMasRecienteYGeneraAlertas() {
        Instant antes = Instant.parse("2026-11-20T10:00:00Z");
        Instant despues = Instant.parse("2026-11-20T11:00:00Z");

        service.registrarStock(stock("s1", 1L, 20, antes));
        service.registrarStock(stock("s2", 1L, 3, despues));      // ahora esta bajo el umbral
        service.registrarStock(stock("s3", 1L, 50, antes));       // llega tarde: no pisa al mas reciente
        service.registrarStock(stock("s4", 2L, 40, despues));

        ResumenReporte r = service.resumen();

        assertEquals(2, r.stockActual().size());
        assertEquals(1, r.alertasStockBajo().size());
        assertEquals(3, r.alertasStockBajo().get(0).stock());
    }
}
