package com.pedidos360.backend.model;

public enum EstadoPedido {
    CREADO, ACEPTADO, EN_PREPARACION, DESPACHADO, ENTREGADO, CANCELADO;

    public boolean puedePasarA(EstadoPedido destino) {
        return switch (this) {
            case CREADO -> destino == ACEPTADO || destino == CANCELADO;
            case ACEPTADO -> destino == EN_PREPARACION;
            case EN_PREPARACION -> destino == DESPACHADO;
            case DESPACHADO -> destino == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
