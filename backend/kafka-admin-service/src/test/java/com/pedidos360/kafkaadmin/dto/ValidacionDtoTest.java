package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacionDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void topicoValidoPasa() {
        assertTrue(validator.validate(new TopicRequest("demo.eventos", 3, 3, Map.of("retention.ms", "1000"))).isEmpty());
    }

    @Test
    void topicoConNombreVacioOCaracteresRarosEsInvalido() {
        assertFalse(validator.validate(new TopicRequest("", 3, 3, null)).isEmpty());
        assertFalse(validator.validate(new TopicRequest("mi topico/ñ", 3, 3, null)).isEmpty());
    }

    @Test
    void topicoConParticionesOReplicasFueraDeRangoEsInvalido() {
        assertFalse(validator.validate(new TopicRequest("ok", 0, 3, null)).isEmpty());
        assertFalse(validator.validate(new TopicRequest("ok", 3, 0, null)).isEmpty());
        assertFalse(validator.validate(new TopicRequest("ok", null, 3, null)).isEmpty());
    }

    @Test
    void actualizacionDeConfigSinContenidoEsInvalida() {
        assertFalse(validator.validate(new ConfigUpdateRequest(Map.of())).isEmpty());
        assertTrue(validator.validate(new ConfigUpdateRequest(Map.of("retention.ms", "1"))).isEmpty());
    }

    @Test
    void particionesDebenSerPositivas() {
        assertFalse(validator.validate(new PartitionsRequest(0)).isEmpty());
        assertTrue(validator.validate(new PartitionsRequest(6)).isEmpty());
    }
}
