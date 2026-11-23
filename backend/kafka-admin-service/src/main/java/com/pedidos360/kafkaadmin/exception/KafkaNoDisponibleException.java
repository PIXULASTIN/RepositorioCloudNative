package com.pedidos360.kafkaadmin.exception;

/** No se pudo hablar con los brokers (caidos, timeout o operacion interrumpida). */
public class KafkaNoDisponibleException extends RuntimeException {
    public KafkaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
