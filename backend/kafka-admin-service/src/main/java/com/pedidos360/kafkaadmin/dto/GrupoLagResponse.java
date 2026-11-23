package com.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Lag por particion = offset final del log - offset confirmado por el grupo. */
public record GrupoLagResponse(String groupId, long lagTotal, List<ParticionLag> particiones) {
}
