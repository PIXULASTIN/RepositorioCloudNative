package com.pedidos360.kafkaadmin.exception;

/** Operacion rechazada por el estado actual de Kafka (topico ya existe, grupo con miembros, topico protegido...). */
public class KafkaConflictException extends RuntimeException {
    public KafkaConflictException(String mensaje) {
        super(mensaje);
    }
}
