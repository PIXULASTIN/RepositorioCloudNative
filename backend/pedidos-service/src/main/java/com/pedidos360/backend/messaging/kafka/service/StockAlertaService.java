package com.pedidos360.backend.messaging.kafka.service;

import com.pedidos360.backend.messaging.kafka.dto.StockKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Regla de negocio: avisar cuando el stock de un producto queda bajo el umbral. No conoce Kafka. */
@Service
public class StockAlertaService {

    private static final Logger log = LoggerFactory.getLogger(StockAlertaService.class);

    private final int umbralBajo;

    public StockAlertaService(@Value("${app.stock.umbral-bajo:5}") int umbralBajo) {
        this.umbralBajo = umbralBajo;
    }

    /** @return true si se genero una alerta de stock bajo. */
    public boolean evaluar(StockKafkaEvent evento) {
        if (evento.stockActual() <= umbralBajo) {
            log.warn("[STOCK-ALERTA] producto={} ({}) stock={} <= umbral={} motivo={}",
                    evento.productoId(), evento.nombre(), evento.stockActual(), umbralBajo, evento.motivo());
            return true;
        }
        log.info("[STOCK] producto={} ({}) stock={} motivo={}",
                evento.productoId(), evento.nombre(), evento.stockActual(), evento.motivo());
        return false;
    }

    public int getUmbralBajo() {
        return umbralBajo;
    }
}
