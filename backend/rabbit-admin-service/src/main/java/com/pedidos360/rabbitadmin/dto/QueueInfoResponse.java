package com.pedidos360.rabbitadmin.dto;

public record QueueInfoResponse(String name, int messageCount, int consumerCount) {
}
