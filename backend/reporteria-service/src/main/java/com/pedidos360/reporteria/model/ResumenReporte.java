package com.pedidos360.reporteria.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Reporte en tiempo real construido a partir de los eventos de Kafka. */
public record ResumenReporte(
        Map<String, Long> pedidosPorEstado,
        double ingresosAceptados,
        List<ProductoVendido> productosMasVendidos,
        List<StockProducto> stockActual,
        List<StockProducto> alertasStockBajo,
        long eventosPedidosProcesados,
        long eventosStockProcesados,
        long eventosDuplicadosIgnorados,
        Instant ultimaActualizacion) {

    public record ProductoVendido(String producto, long unidades) {
    }

    public record StockProducto(Long productoId, String nombre, int stock, Instant actualizado) {
    }
}
