package com.pedidos360.backend.messaging.kafka.consumer;

import com.pedidos360.backend.messaging.kafka.dto.StockKafkaEvent;
import com.pedidos360.backend.messaging.kafka.error.EventoInvalidoException;
import com.pedidos360.backend.messaging.kafka.service.StockAlertaService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consumidor del topico catalogo.stock (grupo "pedidos-stock"). Solo se ocupa de la mensajeria:
 * valida el evento y delega la regla de negocio en StockAlertaService.
 */
@Component
public class StockEventosListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventosListener.class);

    private final StockAlertaService alertaService;
    private final Validator validator;

    public StockEventosListener(StockAlertaService alertaService, Validator validator) {
        this.alertaService = alertaService;
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.stock.name}",
            groupId = "${app.kafka.consumer.groups.pedidos-stock}",
            containerFactory = "stockListenerContainerFactory")
    public void onStock(ConsumerRecord<String, StockKafkaEvent> record) {
        StockKafkaEvent evento = record.value();
        if (evento == null) {   // tombstone de un topico compactado: no hay nada que procesar
            log.info("[KAFKA-CONSUMER] tombstone key={} topico={}", record.key(), record.topic());
            return;
        }
        validar(evento);
        log.info("[KAFKA-CONSUMER] grupo=pedidos-stock topico={} particion={} offset={} producto={}",
                record.topic(), record.partition(), record.offset(), evento.productoId());
        alertaService.evaluar(evento);
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
