package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ParticionInfo(int partition, Integer leader, List<Integer> replicas, List<Integer> isr) {
}
