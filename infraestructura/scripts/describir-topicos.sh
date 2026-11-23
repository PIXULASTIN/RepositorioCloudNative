#!/bin/bash
# Muestra particiones, lider, replicas, ISR y configuracion de los topicos.
#   Uso:  bash infraestructura/scripts/describir-topicos.sh [nombre-del-topico]
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka1:29092,kafka2:29093,kafka3:29094}"
TOPICO="${1:-}"

if [ -n "$TOPICO" ]; then
  docker exec kafka1 kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$TOPICO"
else
  docker exec kafka1 kafka-topics --bootstrap-server "$BOOTSTRAP" --describe
fi
