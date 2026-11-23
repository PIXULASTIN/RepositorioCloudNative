package com.pedidos360.reporteria.service;

import com.pedidos360.reporteria.messaging.dto.PedidoKafkaEvent;
import com.pedidos360.reporteria.messaging.dto.StockKafkaEvent;
import com.pedidos360.reporteria.model.ResumenReporte;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Regla de negocio de la reporteria. Acumula metricas en memoria a partir de los eventos
 * (los consumidores lo invocan desde varios hilos, por eso usa estructuras concurrentes).
 * Ignora eventos repetidos (mismo eventId), porque Kafka entrega "al menos una vez".
 */
@Service
public class ReporteService {

    private static final int MAX_EVENTOS_RECORDADOS = 10_000;
    private static final int TOP_PRODUCTOS = 5;

    private final int umbralStockBajo;

    private final Map<String, LongAdder> pedidosPorEstado = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> unidadesPorProducto = new ConcurrentHashMap<>();
    private final Map<Long, ResumenReporte.StockProducto> stock = new ConcurrentHashMap<>();
    private final DoubleAdder ingresosAceptados = new DoubleAdder();

    private final AtomicLong eventosPedidos = new AtomicLong();
    private final AtomicLong eventosStock = new AtomicLong();
    private final AtomicLong duplicados = new AtomicLong();
    private volatile Instant ultimaActualizacion;

    private final Set<String> eventosVistos = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_EVENTOS_RECORDADOS;
        }
    });

    public ReporteService(@Value("${app.stock.umbral-bajo:5}") int umbralStockBajo) {
        this.umbralStockBajo = umbralStockBajo;
    }

    /** @return true si el evento se proceso; false si era un duplicado. */
    public boolean registrarPedido(PedidoKafkaEvent evento) {
        if (!marcarVisto(evento.eventId())) {
            return false;
        }
        pedidosPorEstado.computeIfAbsent(evento.estado(), k -> new LongAdder()).increment();
        if ("ACEPTADO".equals(evento.estado())) {
            ingresosAceptados.add(evento.total());
            if (evento.items() != null) {
                evento.items().forEach(i -> {
                    String nombre = i.nombreProducto() != null ? i.nombreProducto() : "producto-" + i.productoId();
                    int cantidad = i.cantidad() == null ? 0 : i.cantidad();
                    unidadesPorProducto.computeIfAbsent(nombre, k -> new LongAdder()).add(cantidad);
                });
            }
        }
        eventosPedidos.incrementAndGet();
        ultimaActualizacion = Instant.now();
        return true;
    }

    /** @return true si el evento se proceso; false si era un duplicado. */
    public boolean registrarStock(StockKafkaEvent evento) {
        if (!marcarVisto(evento.eventId())) {
            return false;
        }
        ResumenReporte.StockProducto nuevo = new ResumenReporte.StockProducto(
                evento.productoId(), evento.nombre(), evento.stockActual(), evento.fecha());
        // conserva siempre el estado mas reciente de cada producto
        stock.merge(evento.productoId(), nuevo,
                (actual, entrante) -> entrante.actualizado().isBefore(actual.actualizado()) ? actual : entrante);
        eventosStock.incrementAndGet();
        ultimaActualizacion = Instant.now();
        return true;
    }

    public ResumenReporte resumen() {
        Map<String, Long> porEstado = new TreeMap<>();
        pedidosPorEstado.forEach((estado, n) -> porEstado.put(estado, n.sum()));

        List<ResumenReporte.ProductoVendido> top = unidadesPorProducto.entrySet().stream()
                .map(e -> new ResumenReporte.ProductoVendido(e.getKey(), e.getValue().sum()))
                .sorted(Comparator.comparingLong(ResumenReporte.ProductoVendido::unidades).reversed()
                        .thenComparing(ResumenReporte.ProductoVendido::producto))
                .limit(TOP_PRODUCTOS)
                .toList();

        List<ResumenReporte.StockProducto> stockActual = stock.values().stream()
                .sorted(Comparator.comparing(ResumenReporte.StockProducto::nombre))
                .toList();
        List<ResumenReporte.StockProducto> alertas = stockActual.stream()
                .filter(s -> s.stock() <= umbralStockBajo)
                .toList();

        return new ResumenReporte(porEstado, ingresosAceptados.sum(), top, stockActual, alertas,
                eventosPedidos.get(), eventosStock.get(), duplicados.get(), ultimaActualizacion);
    }

    private boolean marcarVisto(String eventId) {
        synchronized (eventosVistos) {
            boolean nuevo = eventosVistos.add(eventId);
            if (!nuevo) {
                duplicados.incrementAndGet();
            }
            return nuevo;
        }
    }
}
