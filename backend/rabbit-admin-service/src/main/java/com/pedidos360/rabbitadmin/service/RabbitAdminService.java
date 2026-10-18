package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }


    public QueueInfoResponse crearCola(QueueRequest req) {
        validarNombreNoReservado(req.name());
        QueueBuilder qb = (req.durable() == null || req.durable())
                ? QueueBuilder.durable(req.name())
                : QueueBuilder.nonDurable(req.name());
        if (req.messageTtlMs() != null) {
            qb.ttl(req.messageTtlMs().intValue());
        }
        if (req.maxLength() != null) {
            qb.maxLength(req.maxLength());
        }
        if (req.deadLetterExchange() != null && !req.deadLetterExchange().isBlank()) {
            qb.deadLetterExchange(req.deadLetterExchange());
            if (req.deadLetterRoutingKey() != null && !req.deadLetterRoutingKey().isBlank()) {
                qb.deadLetterRoutingKey(req.deadLetterRoutingKey());
            }
        } else if (req.deadLetterRoutingKey() != null && !req.deadLetterRoutingKey().isBlank()) {
            throw new IllegalArgumentException("deadLetterRoutingKey requiere deadLetterExchange");
        }
        amqpAdmin.declareQueue(qb.build());
        log.info("[ADMIN] Cola creada: {}", req.name());
        return infoCola(req.name());
    }

    public QueueInfoResponse infoCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new NoSuchElementException("La cola no existe: " + nombre);
        }
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre) {
        validarNombreNoReservado(nombre);
        infoCola(nombre); 
        amqpAdmin.deleteQueue(nombre);
        log.info("[ADMIN] Cola eliminada: {}", nombre);
    }

    // ---------------- Exchanges ----------------

    public void crearExchange(ExchangeRequest req) {
        validarNombreNoReservado(req.name());
        boolean durable = req.durable() == null || req.durable();
        Exchange exchange = switch (req.type()) {
            case "direct" -> new DirectExchange(req.name(), durable, false);
            case "topic" -> new TopicExchange(req.name(), durable, false);
            case "fanout" -> new FanoutExchange(req.name(), durable, false);
            case "headers" -> new HeadersExchange(req.name(), durable, false);
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + req.type());
        };
        amqpAdmin.declareExchange(exchange);
        log.info("[ADMIN] Exchange creado: {} ({})", req.name(), req.type());
    }

    public void eliminarExchange(String nombre) {
        validarNombreNoReservado(nombre);
        if (!amqpAdmin.deleteExchange(nombre)) {
            throw new NoSuchElementException("El exchange no existe: " + nombre);
        }
        log.info("[ADMIN] Exchange eliminado: {}", nombre);
    }


    public void crearBinding(BindingRequest req) {
        amqpAdmin.declareBinding(aBinding(req));
        log.info("[ADMIN] Binding creado: {} <- {} [{}]", req.queue(), req.exchange(), req.routingKey());
    }

    public void eliminarBinding(BindingRequest req) {
        amqpAdmin.removeBinding(aBinding(req));
        log.info("[ADMIN] Binding eliminado: {} <- {} [{}]", req.queue(), req.exchange(), req.routingKey());
    }


    private Binding aBinding(BindingRequest req) {
        infoCola(req.queue()); // 404 si la cola no existe
        String rk = req.routingKey() == null ? "" : req.routingKey();
        return new Binding(req.queue(), Binding.DestinationType.QUEUE, req.exchange(), rk, null);
    }

    private void validarNombreNoReservado(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        if (nombre.startsWith("amq.")) {
            throw new IllegalArgumentException("Los nombres que empiezan con 'amq.' estan reservados por RabbitMQ");
        }
    }
}
