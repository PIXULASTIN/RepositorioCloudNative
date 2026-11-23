package com.pedidos360.kafkaadmin.dto;

public record ParticionLag(String topic, int partition, long currentOffset, long logEndOffset, long lag) {
}
