package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record MiembroInfo(String consumerId, String clientId, String host, List<String> particiones) {
}
