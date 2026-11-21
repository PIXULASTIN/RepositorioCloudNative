#!/bin/bash
# Crea los 2 topicos del proyecto (3 particiones, factor de replicacion 3).
# Es idempotente: se puede ejecutar varias veces. Tambien sirve para correrlo a mano.
#   BOOTSTRAP por defecto: los 3 brokers internos de Docker.

set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka1:29092,kafka2:29093,kafka3:29094}"

echo "[crear-topicos] Bootstrap: $BOOTSTRAP"

# Topico 1: eventos de pedidos. Se conservan 7 dias (retention.ms = 604800000).
kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic pedidos.eventos \
  --partitions 3 \
  --replication-factor 3 \
  --config cleanup.policy=delete \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2

# Topico 2: stock por producto. Compactado: queda el ultimo evento de cada productoId.
kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
  --topic catalogo.stock \
  --partitions 3 \
  --replication-factor 3 \
  --config cleanup.policy=compact \
  --config min.cleanable.dirty.ratio=0.1 \
  --config min.insync.replicas=2

echo "[crear-topicos] Topicos existentes:"
kafka-topics --bootstrap-server "$BOOTSTRAP" --list
