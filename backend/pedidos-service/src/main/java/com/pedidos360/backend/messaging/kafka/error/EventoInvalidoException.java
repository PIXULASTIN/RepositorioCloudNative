package com.pedidos360.backend.messaging.kafka.error;

/** Evento con datos invalidos. No se reintenta: va directo a la DLT. */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
