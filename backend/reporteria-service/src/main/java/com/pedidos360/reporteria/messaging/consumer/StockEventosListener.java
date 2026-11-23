package com.pedidos360.reporteria.messaging.consumer;

import com.pedidos360.reporteria.messaging.dto.StockKafkaEvent;
import com.pedidos360.reporteria.messaging.error.EventoInvalidoException;
import com.pedidos360.reporteria.service.ReporteService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/** Consumidor del topico catalogo.stock (grupo reporteria-stock). */
@Component
public class StockEventosListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventosListener.class);

    private final ReporteService reporteService;
    private final Validator validator;

    public StockEventosListener(ReporteService reporteService, Validator validator) {
        this.reporteService = reporteService;
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.stock}",
            groupId = "${app.kafka.consumer.groups.stock}",
            containerFactory = "stockListenerContainerFactory")
    public void onStock(ConsumerRecord<String, StockKafkaEvent> record) {
        StockKafkaEvent evento = record.value();
        if (evento == null) {   // tombstone de un topico compactado
            log.info("[KAFKA-CONSUMER] tombstone key={} topico={}", record.key(), record.topic());
            return;
        }
        validar(evento);
        boolean procesado = reporteService.registrarStock(evento);
        log.info("[KAFKA-CONSUMER] grupo=reporteria-stock topico={} particion={} offset={} producto={} stock={} {}",
                record.topic(), record.partition(), record.offset(), evento.productoId(), evento.stockActual(),
                procesado ? "procesado" : "duplicado-ignorado");
    }

    private void validar(StockKafkaEvent evento) {
        Set<ConstraintViolation<StockKafkaEvent>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(e -> e.getPropertyPath() + " " + e.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new EventoInvalidoException("Evento de stock invalido: " + detalle);
        }
    }
}
