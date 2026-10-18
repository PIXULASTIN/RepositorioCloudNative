package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidacionDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void colaConNombreVacioEsInvalida() {
        assertFalse(validator.validate(new QueueRequest("", null, null, null, null, null)).isEmpty());
        assertFalse(validator.validate(new QueueRequest("   ", null, null, null, null, null)).isEmpty());
    }

    @Test
    void colaConCaracteresRarosEsInvalida() {
        assertFalse(validator.validate(new QueueRequest("mi cola/ñ", null, null, null, null, null)).isEmpty());
    }

    @Test
    void colaConTtlONegativoEsInvalida() {
        assertFalse(validator.validate(new QueueRequest("ok", null, -5L, null, null, null)).isEmpty());
        assertFalse(validator.validate(new QueueRequest("ok", null, null, 0L, null, null)).isEmpty());
    }

    @Test
    void colaValidaPasa() {
        assertTrue(validator.validate(new QueueRequest("pedidos.demo.queue", true, 60000L, 100L, null, null)).isEmpty());
    }

    @Test
    void exchangeConTipoInvalidoEsInvalido() {
        assertFalse(validator.validate(new ExchangeRequest("ex", "random", true)).isEmpty());
        assertTrue(validator.validate(new ExchangeRequest("ex", "topic", true)).isEmpty());
    }

    @Test
    void bindingSinColaOExchangeEsInvalido() {
        assertFalse(validator.validate(new BindingRequest("", "ex", "rk")).isEmpty());
        assertFalse(validator.validate(new BindingRequest("q", "", "rk")).isEmpty());
    }
}
