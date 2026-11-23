package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record GrupoInfoResponse(
        String groupId,
        String estado,
        Integer coordinador,
        List<MiembroInfo> miembros) {
}
