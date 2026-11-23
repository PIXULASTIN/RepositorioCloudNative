package com.pedidos360.reporteria.messaging.consumer;

import com.pedidos360.reporteria.messaging.dto.PedidoKafkaEvent;
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

/**
 * Consumidor del topico pedidos.eventos (grupo reporteria-pedidos, 3 hilos = 3 particiones).
 * Solo maneja la mensajeria: valida el evento y delega en ReporteService.
 */
@Component
public class PedidoEventosListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventosListener.class);

    private final ReporteService reporteService;
    private final Validator validator;

    public PedidoEventosListener(ReporteService reporteService, Validator validator) {
        this.reporteService = reporteService;
        this.validator = validator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.pedidos}",
            groupId = "${app.kafka.consumer.groups.pedidos}",
            containerFactory = "pedidosListenerContainerFactory")
    public void onPedido(ConsumerRecord<String, PedidoKafkaEvent> record) {
        PedidoKafkaEvent evento = record.value();
        if (evento == null) {
            log.warn("[KAFKA-CONSUMER] mensaje sin valor ignorado key={} topico={}", record.key(), record.topic());
            return;
        }
        validar(evento);
        boolean procesado = reporteService.registrarPedido(evento);
        log.info("[KAFKA-CONSUMER] grupo=reporteria-pedidos topico={} particion={} offset={} pedido={} estado={} {}",
                record.topic(), record.partition(), record.offset(), evento.pedidoId(), evento.estado(),
                procesado ? "procesado" : "duplicado-ignorado");
    }

    private void validar(PedidoKafkaEvent evento) {
        Set<ConstraintViolation<PedidoKafkaEvent>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            String detalle = errores.stream()
                    .map(e -> e.getPropertyPath() + " " + e.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new EventoInvalidoException("Evento de pedido invalido: " + detalle);
        }
    }
}
