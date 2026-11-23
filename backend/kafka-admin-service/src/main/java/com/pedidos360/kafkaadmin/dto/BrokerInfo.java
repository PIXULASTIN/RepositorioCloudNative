package com.pedidos360.kafkaadmin.dto;

public record BrokerInfo(int id, String host, int port, String rack) {
}
