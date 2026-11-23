package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ClusterInfoResponse(String clusterId, Integer controllerId, List<BrokerInfo> brokers) {
}
