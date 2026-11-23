package com.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

public record TopicInfoResponse(
        String name,
        int partitions,
        int replicationFactor,
        List<ParticionInfo> particiones,
        Map<String, String> configuraciones) {
}
