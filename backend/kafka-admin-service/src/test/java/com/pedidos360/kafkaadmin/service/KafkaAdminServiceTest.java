package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.dto.GrupoLagResponse;
import com.pedidos360.kafkaadmin.dto.TopicRequest;
import com.pedidos360.kafkaadmin.exception.KafkaConflictException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private Admin admin;

    private KafkaAdminService service() {
        return new KafkaAdminService(admin, 2, List.of("pedidos.eventos", "catalogo.stock"));
    }

    @Test
    void listarTopicos_retornaLosNombresOrdenados() {
        ListTopicsResult resultado = mock(ListTopicsResult.class);
        when(resultado.names()).thenReturn(KafkaFuture.completedFuture(Set.of("catalogo.stock", "pedidos.eventos")));
        when(admin.listTopics()).thenReturn(resultado);

        assertEquals(List.of("catalogo.stock", "pedidos.eventos"), service().listarTopicos());
    }

    @Test
    void crearTopico_rechazaNombresReservadosDeKafka() {
        TopicRequest req = new TopicRequest("__interno", 3, 3, null);

        assertThrows(IllegalArgumentException.class, () -> service().crearTopico(req));
    }

    @Test
    void crearTopico_rechazaConfiguracionesNoPermitidas() {
        TopicRequest req = new TopicRequest("demo", 3, 3, Map.of("unclean.leader.election.enable", "true"));

        assertThrows(IllegalArgumentException.class, () -> service().crearTopico(req));
    }

    @Test
    void eliminarTopico_noPermiteBorrarLosTopicosProtegidos() {
        assertThrows(KafkaConflictException.class, () -> service().eliminarTopico("pedidos.eventos"));
    }

    @Test
    void actualizarConfig_exigeAlMenosUnaConfiguracion() {
        assertThrows(IllegalArgumentException.class, () -> service().actualizarConfig("demo", Map.of()));
    }

    @Test
    void lagGrupo_calculaOffsetFinalMenosOffsetConfirmado() {
        TopicPartition tp = new TopicPartition("pedidos.eventos", 0);

        ListConsumerGroupOffsetsResult confirmados = mock(ListConsumerGroupOffsetsResult.class);
        when(confirmados.partitionsToOffsetAndMetadata())
                .thenReturn(KafkaFuture.completedFuture(Map.of(tp, new OffsetAndMetadata(7L))));
        when(admin.listConsumerGroupOffsets("reporteria-pedidos")).thenReturn(confirmados);

        ListOffsetsResult finales = mock(ListOffsetsResult.class);
        when(finales.all()).thenReturn(KafkaFuture.completedFuture(
                Map.of(tp, new ListOffsetsResult.ListOffsetsResultInfo(10L, 0L, Optional.empty()))));
        when(admin.listOffsets(anyMap())).thenReturn(finales);

        GrupoLagResponse lag = service().lagGrupo("reporteria-pedidos");

        assertEquals(3, lag.lagTotal());
        assertEquals(1, lag.particiones().size());
        assertEquals(7, lag.particiones().get(0).currentOffset());
        assertEquals(10, lag.particiones().get(0).logEndOffset());
    }

    @Test
    void lagGrupo_sinOffsetsLanzaNoEncontrado() {
        ListConsumerGroupOffsetsResult vacio = mock(ListConsumerGroupOffsetsResult.class);
        when(vacio.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        when(admin.listConsumerGroupOffsets("fantasma")).thenReturn(vacio);

        assertThrows(NoSuchElementException.class, () -> service().lagGrupo("fantasma"));
    }
}
